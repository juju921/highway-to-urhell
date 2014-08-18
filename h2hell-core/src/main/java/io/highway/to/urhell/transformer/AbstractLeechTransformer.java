package io.highway.to.urhell.transformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractLeechTransformer implements ClassFileTransformer {

    protected final Logger log = LoggerFactory
            .getLogger(this.getClass());
    private final String classNameToTransformNormalized;
    private final String classNameToTransform;

    private List<String> importPackages = new ArrayList<String>();

    public AbstractLeechTransformer(String classNameToTransform) {
        this.classNameToTransform = classNameToTransform;
        this.classNameToTransformNormalized = classNameToTransform.replace(".", "/");

        addImportPackage("io.highway.to.urhell");
        addImportPackage("io.highway.to.urhell.domain");
    }

    protected void addImportPackage(String importPackage) {
        this.importPackages.add(importPackage);
    }

    public byte[] transform(ClassLoader loader, String className,
                            Class classBeingRedefined, ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {


        if (className.startsWith(classNameToTransform)) {
            log.info("Going to Transform {} with {}", classNameToTransform, this.getClass());
            try {
                ClassPool cp = ClassPool.getDefault();
                cp.appendClassPath(new LoaderClassPath(loader));
                for (String importPackage : importPackages) {
                    cp.importPackage(importPackage);
                }
                CtClass cc = cp
                        .get(classNameToTransformNormalized);

                doTransform(cc);

                classfileBuffer = cc.toBytecode();
                cc.detach();

            } catch (NotFoundException ex) {
                log.error("not found ", ex);
            } catch (Exception ex) {
                log.error("Fail to Transform {} with {}", classNameToTransform, this.getClass(), ex);
            }
        }

        return classfileBuffer;
    }

    protected abstract void doTransform(CtClass cc) throws Exception;


}