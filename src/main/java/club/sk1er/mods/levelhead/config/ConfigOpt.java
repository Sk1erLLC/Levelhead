package club.sk1er.mods.levelhead.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by mitchellkatz on 12/23/17. Designed for production use on Levelhead
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigOpt {

    String comment() default "";

    boolean ignore() default false;


}