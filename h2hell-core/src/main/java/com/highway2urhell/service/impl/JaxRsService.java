package com.highway2urhell.service.impl;

import com.highway2urhell.VersionUtils;
import com.highway2urhell.domain.*;
import com.highway2urhell.domain.HttpMethod;
import com.highway2urhell.service.AbstractLeechService;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;


import javax.ws.rs.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class JaxRsService extends AbstractLeechService {

    public static final String FRAMEWORK_NAME = "JAX_RS";

    public JaxRsService() {
        super(FRAMEWORK_NAME, VersionUtils.getVersion(
                "javax.ws.rs.core.Application", "javax.ws.rs",
                "javax.ws.rs-api"));
        setTriggerAtStartup(true);
    }

    @Override
    protected void gatherData(List<EntryPathData> incoming) {
        if (!getFrameworkInformations().getVersion().equals(
                VersionUtils.NO_FRAMEWORK)) {
            // scan
            LOGGER.info("Start Scan reflections JAX-RS ! ");
            Reflections reflections = new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forClassLoader()));
            LOGGER.info("End Scan reflections JAX-RS ! ");
            Set<Class<?>> setPathJAXRS = reflections.getTypesAnnotatedWith(Path.class);
            if (setPathJAXRS != null && !setPathJAXRS.isEmpty()) {
                // Grab all class extends
                for (Class<?> service : setPathJAXRS) {
                    // search annotation type javax.ws.rs.Path
                    for (Annotation annotation : service.getAnnotations()) {
                        if (annotation instanceof Path) {
                            Path remoteAnnotation = (Path) annotation;
                            searchAllMethodPublic(remoteAnnotation, service);
                        }
                    }
                }
            }
        }
    }

    private void searchAllMethodPublic(Path remoteAnnotation, Class<?> service) {
        String pathForClass = remoteAnnotation.value();
        // Search all method public with annotation Path
        if (service.getDeclaredMethods().length > 0) {
            for (Method m : service.getDeclaredMethods()) {
                Path pMethod = m.getAnnotation(Path.class);
                if (pMethod != null) {
                    addEntryPath(getEntryForClassJAXRS(pathForClass, m, service.getName(), pMethod));
                }
            }
        }
    }

    private EntryPathData getEntryForClassJAXRS(String pathClass, Method m,
                                                String nameClass, Path pMethod) {
        EntryPathData entry = new EntryPathData();
        // method
        if (m.getAnnotation(GET.class) != null) {
            entry.setHttpMethod(HttpMethod.GET);
        } else if (m.getAnnotation(POST.class) != null) {
            entry.setHttpMethod(HttpMethod.POST);
        } else if (m.getAnnotation(PUT.class) != null) {
            entry.setHttpMethod(HttpMethod.PUT);
        } else if (m.getAnnotation(DELETE.class) != null) {
            entry.setHttpMethod(HttpMethod.DELETE);
        } else {
            entry.setHttpMethod(HttpMethod.UNKNOWN);
        }
        //
        entry.setUri(pathClass + pMethod.value());
        entry.setClassName(nameClass);
        entry.setMethodName(m.getName());
        entry.setTypePath(TypePath.DYNAMIC);
        entry.setSignatureName(getInternalSignature(m));
        entry.setListEntryPathData(searchParameterMethod(m.getParameterTypes()));
        return entry;
    }

    private List<EntryPathParam> searchParameterMethod(Class<?>[] tabParam) {
        List<EntryPathParam> listEntryPathData = new ArrayList<EntryPathParam>();
        for (Class<?> mMethod : tabParam) {
            EntryPathParam param = new EntryPathParam();
            param.setKey("");
            param.setTypeParam(TypeParam.PARAM_DATA);
            param.setValue(mMethod.getName());
            listEntryPathData.add(param);
        }
        return listEntryPathData;
    }
}
