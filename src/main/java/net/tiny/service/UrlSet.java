package net.tiny.service;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class UrlSet implements Iterable<URL> {

	private static Logger LOGGER = Logger.getLogger(UrlSet.class.getName());
	private static final ClassLoader SYSTEM = ClassLoader.getSystemClassLoader();

	static URLClassLoader findParent(ClassLoader classLoader) {
		ClassLoader parent = classLoader.getParent();
		if (parent != null) {
			if (parent instanceof URLClassLoader) {
				return (URLClassLoader)parent;
			} else {
				return findParent(parent);
			}
		}
		return null;
	}

	public static List<URL> findUrls(final URLClassLoader classLoader) throws IOException {
		if (null == classLoader)
			return Collections.emptyList();
		URL[] urls = classLoader.getURLs();
		if(urls.length > 1) {
			// On maven Test surefire-booter class loader found only one temp jar.
			LOGGER.info(String.format("[ClassFinder] - Found %d classe(s) by the class loader '%s'",
					urls.length, classLoader.getClass().getName()));
			return Arrays.asList(urls);
		} else {
			LOGGER.warning(String.format("[ClassFinder] - Can not found %d classe(s) by the class loader : '%s'",
					urls.length, classLoader.getClass().getName()));
			return findUrls((URLClassLoader)findParent(classLoader));
		}
	}

	public static Set<URL> getUrls(final ClassLoader classLoader) throws IOException {
		List<URL> urls;
		if (classLoader instanceof URLClassLoader) {
			urls = findUrls((URLClassLoader)classLoader);
		} else {
			urls = findUrls(findParent(classLoader));
		}
		if (urls.size() < 1) {
			// On maven Test surefire-booter class loader found only one temp jar.
			LOGGER.warning(String.format("[ClassFinder] - Can not found %d classe(s) by the class loader : '%s'",
					urls.size(), classLoader.getClass().getName()));
			if (SYSTEM instanceof URLClassLoader) {
				urls = findUrls((URLClassLoader)SYSTEM);
			}
		}

		final Set<URL> set = new LinkedHashSet<>();

    	LinkedHashSet<URL> jars = new LinkedHashSet<>();
    	LinkedHashSet<URL> https = new LinkedHashSet<>();
		for (final URL url : urls) {
        	if(url.getProtocol().equals("file")) {
        		if(url.getPath().endsWith(".jar")) {
        			jars.add(url);
        		} else {
        			set.add(url);
        		}
        	} else if(url.getProtocol().equals("jar")) {
        		jars.add(url);
        	} else {
        		https.add(url);
        	}
		}

        if(!jars.isEmpty()) {
        	set.addAll(jars);
        }
        if(!https.isEmpty()) {
        	set.addAll(https);
        }
		return set;
	}

    private final Map<String,URL> urls;

    public UrlSet(ClassLoader classLoader) throws IOException {
        this(getUrls(classLoader));
    }

    public UrlSet(URL... urls){
        this(Arrays.asList(urls));
    }
    /**
     * Ignores all URLs that are not "jar" or "file"
     * @param urls
     */
    public UrlSet(Collection<URL> urls){
        this.urls = new HashMap<String,URL>();
        for (URL location : urls) {
        	this.urls.put(location.toExternalForm(), location);
        }
    }

    private UrlSet(Map<String, URL> urls) {
        this.urls = urls;
    }


    public UrlSet include(UrlSet urlSet){
        Map<String, URL> urls = new HashMap<String, URL>(this.urls);
        urls.putAll(urlSet.urls);
        return new UrlSet(urls);
    }


    public UrlSet include(URL url){
        Map<String, URL> urls = new HashMap<String, URL>(this.urls);
        urls.put(url.toExternalForm(), url);
        return new UrlSet(urls);
    }

    public UrlSet exclude(UrlSet urlSet) {
        Map<String, URL> urls = new HashMap<String, URL>(this.urls);
        Map<String, URL> parentUrls = urlSet.urls;
        for (String url : parentUrls.keySet()) {
            urls.remove(url);
        }
        return new UrlSet(urls);
    }

    public UrlSet exclude(URL url) {
        Map<String, URL> urls = new HashMap<String, URL>(this.urls);
        urls.remove(url.toExternalForm());
        return new UrlSet(urls);
    }

    public UrlSet exclude(ClassLoader parent) throws IOException {
        return exclude(new UrlSet(parent));
    }

    public UrlSet exclude(String pattern) {
        return filter(Patterns.valueOf("!" + pattern));
    }

    public Set<URL> getUrls() {
    	LinkedHashSet<URL> jars = new LinkedHashSet<>();
    	LinkedHashSet<URL> classes = new LinkedHashSet<>();
    	LinkedHashSet<URL> https = new LinkedHashSet<>();
        for(final URL url : urls.values()) {
        	if(url.getProtocol().equals("file")) {
        		if(url.getPath().endsWith(".jar")) {
        			jars.add(url);
        		} else {
        			classes.add(url);
        		}
        	} else if(url.getProtocol().equals("jar")) {
        		jars.add(url);
        	} else {
        		https.add(url);
        	}
        }
        LinkedHashSet<URL> result = new LinkedHashSet<>(classes);
        if(!jars.isEmpty()) {
        	result.addAll(jars);
        }
        if(!https.isEmpty()) {
        	result.addAll(https);
        }
        return result;
    }

    public Set<URL> getPaths() {
    	LinkedHashSet<URL> classes = new LinkedHashSet<>();
        for(URL url : urls.values()) {
        	if(url.getProtocol().equals("file") && url.getPath().endsWith("/")) {
       			classes.add(url);
        	}
        }
    	return classes;
    }

    public Set<URL> getJars() {
    	LinkedHashSet<URL> jars = new LinkedHashSet<>();
        for(final URL url : urls.values()) {
        	if(url.getProtocol().equals("file")) {
        		if(url.getPath().endsWith(".jar")) {
        			jars.add(url);
        		}
        	} else if(url.getProtocol().equals("jar")) {
        		jars.add(url);
        	} else {
        		jars.add(url);
        	}
        }
        return jars;
    }

    public int size() {
        return urls.size();
    }

    public Iterator<URL> iterator() {
        return getUrls().iterator();
    }

    public UrlSet filter(Patterns patterns) {
        Map<String, URL> urls = new HashMap<String, URL>();
        for (Map.Entry<String, URL> entry : this.urls.entrySet()) {
            String url = entry.getKey();
            if (patterns.vaild(url.toString())){
                urls.put(url, entry.getValue());
            }
        }
        return new UrlSet(urls);
    }

    public UrlSet matching(String pattern) {
        return filter(Patterns.valueOf(pattern));
    }

    @Override
    public String toString() {
        return super.toString() + "[" + urls.size() + "]";
    }
}
