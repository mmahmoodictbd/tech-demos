package com.unloadbrain.annotation;

import static javax.lang.model.element.ElementKind.FIELD;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import org.springframework.data.jpa.repository.JpaRepository;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AutoService(Processor.class)
public class HibernateEntityProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(GenerateEntity.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (var element : roundEnv.getElementsAnnotatedWith(GenerateEntity.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                continue;
            }

            var classElement = (TypeElement) element;
            var annotation = classElement.getAnnotation(GenerateEntity.class);

            generateEntityClass(classElement, annotation);
            generateRepositoryInterface(annotation);
        }
        return true;
    }
    
    private void generateEntityClass(TypeElement originalClass, GenerateEntity annotation) {
        var classBuilder = TypeSpec.classBuilder(annotation.entityName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Entity.class)
                .addAnnotation(AllArgsConstructor.class)
                .addAnnotation(NoArgsConstructor.class)
                .addAnnotation(ToString.class)
                .addAnnotation(AnnotationSpec.builder(Table.class)
                        .addMember("name", "$S", annotation.tableName())
                        .build());

        // Add deliverId field
        classBuilder.addField(FieldSpec.builder(String.class, "deliveryId", Modifier.PRIVATE).build());

        // Add getter/setter for ID
        classBuilder.addMethod(
                MethodSpec.methodBuilder("getDeliveryId")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(String.class)
                        .addStatement("return this.deliveryId")
                        .build());

        classBuilder.addMethod(
                MethodSpec.methodBuilder("setDeliveryId")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(String.class, "deliveryId")
                        .addStatement("this.deliveryId = deliveryId")
                        .build());

        // Process fields from original class
        originalClass.getEnclosedElements().stream()
                .filter(enclosedElement -> enclosedElement.getKind() == FIELD)
                .forEach(enclosedElement -> addFieldFromOriginalClass((VariableElement) enclosedElement, classBuilder));


        // Write the class to a file
        writeJavaFile(annotation.packageName(), classBuilder.build(), "Failed to write entity class.");
    }

    private void addFieldFromOriginalClass(VariableElement variableElement, TypeSpec.Builder classBuilder) {
        var field = variableElement;
        var fieldName = field.getSimpleName().toString();
        var fieldType = TypeName.get(field.asType());

        var fieldBuilder = FieldSpec.builder(fieldType, fieldName, Modifier.PRIVATE);
        var hasColumnAnnotation = false;

        for (var annotationMirror : field.getAnnotationMirrors()) {
            var annotationType = annotationMirror.getAnnotationType();
            var annotationName = annotationType.asElement().getSimpleName().toString();

            var packageName = processingEnv.getElementUtils()
                    .getPackageOf(annotationType.asElement()).getQualifiedName().toString();
            var simpleName = annotationType.asElement().getSimpleName().toString();

            // Create annotation spec with proper ClassName construction
            AnnotationSpec.Builder annotationBuilder = AnnotationSpec.builder(
                    ClassName.get(packageName, simpleName));

            // Add annotation parameters
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
                    annotationMirror.getElementValues().entrySet()) {
                var paramName = entry.getKey().getSimpleName().toString();
                var paramValue = entry.getValue().toString();

                // Handle different parameter types
                if (paramValue.startsWith("\"") && paramValue.endsWith("\"")) {
                    // String value
                    annotationBuilder.addMember(paramName, "$S",
                            paramValue.substring(1, paramValue.length() - 1));
                } else if (paramValue.endsWith(".class")) {
                    var className = paramValue.substring(0, paramValue.length() - 6);
                    annotationBuilder.addMember(paramName, "$T.class",
                            processingEnv.getElementUtils().getTypeElement(className));
                } else {
                    annotationBuilder.addMember(paramName, paramValue);
                }
            }

            // Add annotation to field
            fieldBuilder.addAnnotation(annotationBuilder.build());

            // Check if this is a JPA Column annotation
            if (annotationName.equals("Column")) {
                hasColumnAnnotation = true;
            }
        }

        // Add default @Column if not already present
        if (!hasColumnAnnotation) {
            fieldBuilder.addAnnotation(
                    AnnotationSpec.builder(Column.class)
                            .addMember("name", "$S", convertCamelToSnakeCase(fieldName))
                            .build());
        }

        // Add field to class
        classBuilder.addField(fieldBuilder.build());

        // Generate getter
        classBuilder.addMethod(
                MethodSpec.methodBuilder("get" + capitalize(fieldName))
                        .addModifiers(Modifier.PUBLIC)
                        .returns(fieldType)
                        .addStatement("return this.$L", fieldName)
                        .build());

        // Generate setter
        classBuilder.addMethod(
                MethodSpec.methodBuilder("set" + capitalize(fieldName))
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(fieldType, fieldName)
                        .addStatement("this.$1L = $1L", fieldName)
                        .build());

    }

    // Helper method to capitalize first letter
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    // Helper method to convert camelCase to snake_case
    private String convertCamelToSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }


    private void generateRepositoryInterface(GenerateEntity annotation) {
        var repoName = annotation.entityName() + "Repository";
        var typeSpec = TypeSpec.interfaceBuilder(repoName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(
                        ParameterizedTypeName.get(
                                ClassName.get(JpaRepository.class),
                                ClassName.get(annotation.packageName(), annotation.entityName()),
                                ClassName.get(UUID.class)
                        )
                ).build();

        writeJavaFile(annotation.packageName(), typeSpec, "Failed to write repository interface: ");
    }

    private void writeJavaFile(final String packageName,
                               final TypeSpec typeSpec,
                               final String exceptionMessage) {
        try {
            var javaFile = JavaFile.builder(packageName, typeSpec).build();
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    exceptionMessage + e.getMessage()
            );
        }
    }

}
