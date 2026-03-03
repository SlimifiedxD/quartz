package org.slimecraft.bedrock.internal;

import org.jetbrains.annotations.ApiStatus;
import org.slimecraft.bedrock.annotation.plugin.Plugin;
import org.slimecraft.bedrock.dependency.LoadOrder;
import org.slimecraft.bedrock.dependency.LoadStage;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class processes annotations for {@link Plugin}s. This is not meant to be used by end users.
 */
@ApiStatus.Internal
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class PluginConfigProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            for (final Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                final Filer filer = processingEnv.getFiler();
                for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
                    try {
                        if (!mirror.getAnnotationType().toString().equals(Plugin.class.getCanonicalName()))
                            continue;
                        final Map<? extends ExecutableElement, ? extends AnnotationValue> values = mirror.getElementValues();
                        final Map<String, Object> data = new LinkedHashMap<>();
                        final TypeElement typeElement = (TypeElement) element;
                        final String qualifiedName = typeElement.getQualifiedName().toString();
                        final TypeElement javaPluginElement = processingEnv.getElementUtils().getTypeElement("org.bukkit.plugin.java.JavaPlugin");
                        if (javaPluginElement == null) continue;
                        if (!processingEnv.getTypeUtils().isAssignable(typeElement.asType(), javaPluginElement.asType())) continue;

                        data.put("main", qualifiedName);

                        for (ExecutableElement method : mirror
                                .getAnnotationType()
                                .asElement()
                                .getEnclosedElements()
                                .stream()
                                .filter(e -> e.getKind() == ElementKind.METHOD)
                                .map(ExecutableElement.class::cast)
                                .toList()) {
                            final String paramName = method.getSimpleName().toString();
                            final AnnotationValue explicitValue = values.get(method);
                            final Object paramValue = explicitValue != null
                                    ? explicitValue.getValue()
                                    : method.getDefaultValue() != null ? method.getDefaultValue().getValue() : null;
                            if (paramValue == null || paramValue instanceof String string && string.isEmpty()) {
                                continue;
                            }
                            if (paramName.equals("apiVersion")) {
                                data.put("api-version", paramValue);
                                continue;
                            } else if (paramName.equals("dependencies")) {
                                final List<? extends AnnotationValue> depValues = (List<? extends AnnotationValue>) paramValue;
                                if (depValues.isEmpty()) continue;
                                final Map<String, Object> depMap = (Map<String, Object>) data.computeIfAbsent("dependencies", k -> new LinkedHashMap<>());
                                for (AnnotationValue value : depValues) {
                                    final AnnotationMirror depMirror = (AnnotationMirror) value.getValue();
                                    String name = null;
                                    LoadOrder loadOrder = null;
                                    boolean required = false;
                                    boolean joinClasspath = false;
                                    LoadStage loadStage = null;
                                    for (ExecutableElement depMethod : depMirror.getAnnotationType()
                                            .asElement()
                                            .getEnclosedElements()
                                            .stream()
                                            .filter(e -> e.getKind() == ElementKind.METHOD)
                                            .map(ExecutableElement.class::cast)
                                            .toList()) {
                                        final String key = depMethod.getSimpleName().toString();
                                        final AnnotationValue valueObj = depMirror.getElementValues().get(depMethod);
                                        final Object entryValue = valueObj != null
                                                ? valueObj.getValue()
                                                : depMethod.getDefaultValue() != null ? depMethod.getDefaultValue().getValue() : null;

                                        switch (key) {
                                            case "value" -> name = (String) entryValue;
                                            case "loadOrder" -> {
                                                final VariableElement enumConst = (VariableElement) entryValue;
                                                loadOrder = LoadOrder.valueOf(enumConst.getSimpleName().toString());
                                            }
                                            case "required" -> required = (boolean) entryValue;
                                            case "joinClasspath" -> joinClasspath = (boolean) entryValue;
                                            case "loadStage" -> {
                                                final VariableElement enumConst = (VariableElement) entryValue;
                                                loadStage = LoadStage.valueOf(enumConst.getSimpleName().toString());
                                            }
                                        }
                                    }
                                    final Map<String, Object> mapToUse = (Map<String, Object>) (loadStage == LoadStage.BOOTSTRAP
                                                                                ? depMap.computeIfAbsent("bootstrap", k -> new LinkedHashMap<>())
                                                                                : depMap.computeIfAbsent("server", k -> new LinkedHashMap<>()));
                                    final Map<String, Object> pluginDep = (Map<String, Object>) mapToUse.computeIfAbsent(name, k -> new LinkedHashMap<>());
                                    pluginDep.put("load", loadOrder.toString());
                                    pluginDep.put("required", required);
                                    pluginDep.put("join-classpath", joinClasspath);
                                }
                                continue;
                            } else if (paramName.equals("bootstrapper") || paramName.equals("loader")) {
                                final DeclaredType declaredType = (DeclaredType) paramValue;
                                final TypeElement typeElementValue = (TypeElement) declaredType.asElement();

                                final Types typeUtils = processingEnv.getTypeUtils();
                                final Elements elementUtils = processingEnv.getElementUtils();
                                final TypeMirror bootstrapType = elementUtils.getTypeElement("io.papermc.paper.plugin.bootstrap.PluginBootstrap").asType();
                                final TypeMirror pluginLoaderType = elementUtils.getTypeElement("io.papermc.paper.plugin.loader.PluginLoader").asType();
                                final TypeMirror typeMirror = declaredType.asElement().asType();
                                if (typeUtils.isSameType(typeMirror, bootstrapType) || typeUtils.isSameType(typeMirror, pluginLoaderType)) continue;
                                data.put(paramName, typeElementValue.getQualifiedName().toString());
                                continue;
                            } else if (paramName.equals("value")) {
                                data.put("name", paramValue);
                                continue;
                            }
                            data.put(paramName, paramValue);
                        }

                        final FileObject yamlFile = filer.createResource(
                                StandardLocation.CLASS_OUTPUT,
                                "",
                                "paper-plugin.yml"
                        );

                        final DumperOptions options = new DumperOptions();

                        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                        options.setPrettyFlow(true);

                        final Yaml yaml = new Yaml(options);

                        try (final Writer writer = yamlFile.openWriter()) {
                            yaml.dump(data, writer);
                        }

                        final FileObject bedrockInitFile = filer.createSourceFile("org.slimecraft.bedrock.generated.GeneratedBedrockInit");
                        final String codeName = qualifiedName + ".class";
                        try (final Writer writer = bedrockInitFile.openWriter()) {
                            writer.write(
                                    """
                                            package org.slimecraft.bedrock.generated;
                                            
                                            import org.slimecraft.bedrock.internal.Bedrock;
                                            import org.bukkit.plugin.java.JavaPlugin;
                                            
                                            public class GeneratedBedrockInit {
                                                static {
                                                    Bedrock.bedrock().init(JavaPlugin.getProvidingPlugin(%s));
                                                }
                                            }
                                            """.formatted(codeName));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Plugin.class.getName());
    }
}