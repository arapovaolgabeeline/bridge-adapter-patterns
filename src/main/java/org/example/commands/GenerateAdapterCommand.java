package org.example.commands;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.example.interfaces.ICommand;
import org.example.interfaces.IDependency;
import org.example.ioc.IoC;

public class GenerateAdapterCommand implements ICommand {
    public static final String DEFAULT_ADAPTER_POSTFIX = "Adapter";
    private final Class desiredInterface;
    private final Object object;

    public GenerateAdapterCommand(Class desiredInterface, Object object) {
        this.desiredInterface = desiredInterface;
        this.object = object;
    }

    @Override
    public void execute() {
        Method[] declaredMethods = desiredInterface.getDeclaredMethods();

        String className = desiredInterface.getSimpleName()
                .substring(1)
                .concat(DEFAULT_ADAPTER_POSTFIX);

        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(Map.class, String.class, String.class);
        MethodSpec.Builder constructorSpec = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
        constructorSpec.addParameter(parameterizedTypeName, "obj")
                .addStatement("this.obj = obj");

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(desiredInterface)
                .addField(FieldSpec.builder(parameterizedTypeName, "obj", Modifier.PRIVATE, Modifier.FINAL).build())
                .addMethod(constructorSpec.build());

        for (Method declaredMethod : declaredMethods) {
            String name = declaredMethod.getName();

            List<ParameterSpec> parameters = new ArrayList<>();
            for (Parameter parameter : declaredMethod.getParameters()) {
                ParameterSpec parameterSpec = ParameterSpec.builder(parameter.getType(),
                                parameter.getName())
                        .build();
                parameters.add(parameterSpec);
            }
            MethodSpec.Builder methodSpec = MethodSpec.methodBuilder(name)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameters(parameters)
                    .returns(declaredMethod.getReturnType());

            if (name.startsWith("set")) {
                methodSpec.addStatement("((org.example.interfaces.ICommand) org.example.ioc.IoC.resolve(\"" + desiredInterface.getName() + ":" +
                        declaredMethod.getName().substring(3).toLowerCase() +
                        ".set\", new Object[] { obj, arg0 })).execute()", name, declaredMethod.getName());
            } else if (name.startsWith("get")) {
                methodSpec.addStatement("return org.example.ioc.IoC.resolve(\"" + desiredInterface.getName() + ":" +
                                declaredMethod.getName().substring(3).toLowerCase() + ".get\", new Object[] { obj })",
                        name, declaredMethod.getName());
            } else if (name.equals("finish")) {
                methodSpec.addStatement("((org.example.interfaces.ICommand) org.example.ioc.IoC.resolve(\"" +
                        "IoC.Unregister\", new Object[] { \"" + className + "\" })).execute()", name, declaredMethod.getName());
            } else {
                methodSpec.addStatement("throw new UnsupportedOperationException()");
            }

            classBuilder.addMethod(methodSpec.build());
        }

        TypeSpec generatedClass = classBuilder.build();
        JavaFile javaFile = JavaFile.builder("org.example.generated.adapters", generatedClass)
                .build();
        String pathname = "target/generated-sources";
        File directory = new File(pathname);
        if (!directory.exists()) {
            directory.mkdir();
        }

        try {
            javaFile.writeTo(directory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ICommand resolve = IoC.resolve("IoC.Register", new Object[]{className, (IDependency) args -> {
                    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            File file = new File(pathname.toString(), "org/example/generated/adapters/" + className + ".java");

            int run = compiler.run(null, null, null, file.getAbsolutePath());

            while (run != 0) {
                System.out.println("waiting for " + run);
            }

            URLClassLoader classLoader = null;
                    try {
                        classLoader = new URLClassLoader(new URL[]{directory.toURI().toURL()});
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                    Class<?> aClass;

                    try {
                        aClass = classLoader.loadClass("org.example.generated.adapters." + className);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    Constructor<?> declaredConstructor;
                    try {
                        declaredConstructor = aClass.getConstructor(Map.class);
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        return declaredConstructor.newInstance(object);
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }}
        );

        resolve.execute();
    }
}