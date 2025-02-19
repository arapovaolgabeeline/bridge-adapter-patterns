package org.example.commands;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import org.example.interfaces.ICommand;

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

            methodSpec.addStatement("throw new UnsupportedOperationException()");

            classBuilder.addMethod(methodSpec.build());
        }

        TypeSpec generatedClass = classBuilder.build();
        JavaFile javaFile = JavaFile.builder("org.example.generated.adapters", generatedClass)
                .build();
        File directory = new File("target/generated-sources");
        if (!directory.exists()) {
            directory.mkdir();
        }

        try {
            javaFile.writeTo(directory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
