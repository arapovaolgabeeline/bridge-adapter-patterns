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
    public static final String DEFAULT_PACKAGE_NAME = "org.example.generated.adapters";
    public static final String DEFAULT_FOLDER = "target/generated-sources";
    private final Class desiredInterface;
    private final Object object;

    public GenerateAdapterCommand(Class desiredInterface, Object object) {
        this.desiredInterface = desiredInterface;
        this.object = object;
    }

    public static String createAdapterClassName(Class desiredInterface) {
        return desiredInterface.getSimpleName()
                .substring(1)
                .concat(DEFAULT_ADAPTER_POSTFIX);
    }

    @Override
    public void execute() {
        String className = createAdapterClassName(desiredInterface);
        TypeSpec generatedClass = constructAdapter(className);
        File directory = persistAdapterAndRetunFolder(generatedClass);

        ICommand resolve = IoC.resolve("IoC.Register", new Object[]{className, (IDependency) args -> {
                    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                    File file = new File(DEFAULT_FOLDER.toString(), "org/example/generated/adapters/" + className + ".java");

                    compiler.run(null, null, null, file.getAbsolutePath());

                    URLClassLoader classLoader;

                    try {
                        classLoader = new URLClassLoader(new URL[]{directory.toURI().toURL()});
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                    Class<?> aClass;

                    try {
                        aClass = classLoader.loadClass(DEFAULT_PACKAGE_NAME + "." + className);
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

    private static File persistAdapterAndRetunFolder(TypeSpec generatedClass) {
        JavaFile javaFile = JavaFile.builder(DEFAULT_PACKAGE_NAME, generatedClass)
                .build();
        File directory = getDirectory();
        try {
            javaFile.writeTo(directory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return directory;
    }

    private static File getDirectory() {
        File directory = new File(DEFAULT_FOLDER);
        if (!directory.exists()) {
            directory.mkdir();
        }
        return directory;
    }

    private TypeSpec constructAdapter(String className) {
        ParameterizedTypeName fieldType = ParameterizedTypeName.get(Map.class, String.class, String.class);
        MethodSpec constructor = createConstructor(fieldType);

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(desiredInterface)
                .addField(FieldSpec.builder(fieldType, "obj", Modifier.PRIVATE, Modifier.FINAL).build())
                .addMethod(constructor);

        for (Method declaredMethod : desiredInterface.getDeclaredMethods()) {
            MethodSpec method = createMethod(declaredMethod, className);
            classBuilder.addMethod(method);
        }

        TypeSpec generatedClass = classBuilder.build();
        return generatedClass;
    }

    private static MethodSpec createConstructor(ParameterizedTypeName parameterizedTypeName) {
        MethodSpec.Builder constructorSpec = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
        constructorSpec.addParameter(parameterizedTypeName, "obj")
                .addStatement("this.obj = obj");
        MethodSpec constructor = constructorSpec.build();
        return constructor;
    }

    private MethodSpec createMethod(Method declaredMethod, String className) {
        String name = declaredMethod.getName();
        List<ParameterSpec> parameters = prepareMethodsParameters(declaredMethod);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addParameters(parameters)
                .returns(declaredMethod.getReturnType());
        MethodSpec.Builder methodWithStatement = selectStatementForMethod(declaredMethod, name, methodBuilder, className);
        return methodWithStatement.build();
    }

    private static List<ParameterSpec> prepareMethodsParameters(Method declaredMethod) {
        List<ParameterSpec> parameters = new ArrayList<>();
        for (Parameter parameter : declaredMethod.getParameters()) {
            ParameterSpec parameterSpec = ParameterSpec.builder(parameter.getType(),
                            parameter.getName())
                    .build();
            parameters.add(parameterSpec);
        }
        return parameters;
    }

    private MethodSpec.Builder selectStatementForMethod(Method declaredMethod,
                                                        String name,
                                                        MethodSpec.Builder methodBuilder,
                                                        String className) {
        if (name.startsWith("set")) {
            methodBuilder.addStatement(getSetMethodTemplate(declaredMethod), name, declaredMethod.getName());
        } else if (name.startsWith("get")) {
            methodBuilder.addStatement(getGetMethodTemplate(declaredMethod),
                    name, declaredMethod.getName());
        } else if (name.equals("finish")) {
            methodBuilder.addStatement(getFinishMethodTemplate(className), name, declaredMethod.getName());
        } else {
            methodBuilder.addStatement(getDefaultMethodTemplate());
        }
        return methodBuilder;
    }

    private static String getDefaultMethodTemplate() {
        return "throw new UnsupportedOperationException()";
    }

    private static String getFinishMethodTemplate(String className) {
        return "((org.example.interfaces.ICommand) org.example.ioc.IoC.resolve(\"" +
                "IoC.Unregister\", new Object[] { \"" + className + "\" })).execute()";
    }

    private String getGetMethodTemplate(Method declaredMethod) {
        return "return org.example.ioc.IoC.resolve(\"" + desiredInterface.getName() + ":" +
                declaredMethod.getName().substring(3).toLowerCase() + ".get\", new Object[] { obj })";
    }

    private String getSetMethodTemplate(Method declaredMethod) {
        return "((org.example.interfaces.ICommand) org.example.ioc.IoC.resolve(\"" +
                desiredInterface.getName() + ":" +
                declaredMethod.getName().substring(3).toLowerCase() +
                ".set\", new Object[] { obj, arg0 })).execute()";
    }
}