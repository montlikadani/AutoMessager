package hu.montlikadani.AutoMessager.bukkit.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandProcessor {

	/**
	 * @return the name of this command
	 */
	String name() default "";

	/**
	 * @return the permission of this command
	 */
	hu.montlikadani.AutoMessager.bukkit.Perm permission();

}
