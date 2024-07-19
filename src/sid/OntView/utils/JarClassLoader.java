package sid.OntView.utils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;

public class JarClassLoader extends URLClassLoader {
	
	URL url = null; 
	
	public JarClassLoader(URL url) {
	    super(new URL[] { url });
	    this.url = url;
	}
	
	public String getMainClassName() throws IOException {
	    URL u = new URL("jar", "", url + "!/");
	    JarURLConnection uc = (JarURLConnection)u.openConnection();
	    Attributes attr = uc.getMainAttributes();
	    return attr != null
	                   ? attr.getValue(Attributes.Name.MAIN_CLASS)
	                   : null;
	}
	
	public void invokeClass(String name, String[] args)
		    throws ClassNotFoundException,
		           NoSuchMethodException,
		           InvocationTargetException
		{
		    Class c = loadClass(name);
		    Method m = c.getMethod("main", new Class[] { args.getClass() });
		    m.setAccessible(true);
		    int mods = m.getModifiers();
		    if (m.getReturnType() != void.class || !Modifier.isStatic(mods) ||
		        !Modifier.isPublic(mods)) {
		        throw new NoSuchMethodException("main");
		    }
		    try {
		        m.invoke(null, new Object[] { args });
		    } catch (IllegalAccessException e) {
		        // This should not happen, as we have disabled access checks
		    }
		}
}
