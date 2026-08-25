package com.jacafi.tech.shared.lgpd;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field, record component or accessor that carries personal data as defined by
 * Art. 5 I of the LGPD (Lei nº 13.709/2018).
 *
 * <p>The marker serves the transparency principle of Art. 6 VI: it makes the reach of personal
 * data inside the codebase mechanically visible, so an audit does not depend on reading every
 * class to find out where such data lives.
 *
 * <p>Deliberately <em>not</em> named after sensitive data. Sensitive data is a closed category
 * under Art. 5 II — racial or ethnic origin, religious belief, political opinion, union
 * membership, health or sex life, genetic or biometric data. A license plate and a taxpayer
 * registration are personal, not sensitive, and the distinction changes which legal basis
 * applies.
 *
 * <p>Retained at runtime so that a future audit tool, or a serialization filter, can read it by
 * reflection.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.METHOD, ElementType.PARAMETER})
public @interface PersonalData {

    /**
     * Short note on why the annotated element holds personal data, and under which article.
     * Free text, meant for a human reading the code or an audit report.
     */
    String value() default "";
}
