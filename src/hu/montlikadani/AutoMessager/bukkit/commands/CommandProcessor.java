package hu.montlikadani.automessager.bukkit.commands;

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
	hu.montlikadani.automessager.bukkit.Perm permission();

	/**
	 * whenever this command should only be performed with player
	 * 
	 * @return <code>false</code> by default
	 */
	boolean playerOnly() default false;

}
