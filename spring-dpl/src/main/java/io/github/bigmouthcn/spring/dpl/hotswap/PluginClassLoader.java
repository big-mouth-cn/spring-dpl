package io.github.bigmouthcn.spring.dpl.hotswap;

import io.github.bigmouthcn.spring.dpl.PluginRuntimeException;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.jar.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author allen
 * @date 2019/6/27
 * @since 1.0.0
 */
@Slf4j
public class PluginClassLoader extends ClassLoader {

    private final static String LIB_PREFIX = "lib/";
    private final static String JAR_SUFFIX = ".jar";
    private final static String CLASS_SUFFIX = ".class";
    private final static String MAIN_RESOURCE_PREFIX = "main";
    private final static String INNER_PREFIX_SEP = "!";
    private final static String MAIN_RESOURCE_PREFIX_SEP = MAIN_RESOURCE_PREFIX + INNER_PREFIX_SEP;
    private final static int MAIN_RESOURCE_PREFIX_SEP_LEN = MAIN_RESOURCE_PREFIX_SEP.length();

    private final List<String> subJarNameList = Lists.newArrayList();
    private final Map<String, ByteCode> byteCodeCache = Maps.newHashMap();

    private final String pluginJarPath;

    private ProtectionDomain protectionDomain;

    public PluginClassLoader(String pluginJarPath) {
        this(pluginJarPath, ClassLoader.getSystemClassLoader());
    }

    public PluginClassLoader(String pluginJarPath, ClassLoader classLoader) {
        super(classLoader);
        Preconditions.checkArgument(StringUtils.isNotBlank(pluginJarPath));
        this.pluginJarPath = pluginJarPath;
        this.init(pluginJarPath);
    }

    private synchronized void init(String jar) {
        URL url;
        try {
            url = new URL("file:" + jar);
        } catch (MalformedURLException e) {
            throw new PluginRuntimeException("bad url!", e);
        }

        this.protectionDomain = generate(url);
        try {
            this.loadByteCodes(jar);
        } catch (IOException e) {
            throw new PluginRuntimeException("loadByteCodes: ", e);
        }

        this.subJarNameList.add(MAIN_RESOURCE_PREFIX);

        if (log.isDebugEnabled()) {
            log.debug("Plugin [{}] load byte code successful.", jar);
        }
    }

    private ProtectionDomain generate(URL url) {
        CodeSource codeSource = new CodeSource(url, (java.security.cert.Certificate[]) null);
        return new ProtectionDomain(codeSource, null, this, null);
    }

    private void loadByteCodes(String jarFilePath) throws IOException {
        JarFile jar = null;

        try {
            jar = new JarFile(jarFilePath);
            Manifest manifest = jar.getManifest();


            for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
                JarEntry jarEntry = e.nextElement();
                if (jarEntry.isDirectory()) {
                    continue;
                }
                String entryName = jarEntry.getName();
                InputStream inputStream = jar.getInputStream(jarEntry);
                if (null == inputStream) {
                    throw new IOException("Unable to load resource: " + entryName);
                }

                if (isJar(entryName)) {
                    this.loadJarByteCodes(inputStream, entryName);
                } else {
                    this.loadSingleByteCodes(inputStream, entryName, manifest, MAIN_RESOURCE_PREFIX);
                }
            }
        } finally {
            if (null != jar) {
                jar.close();
            }
        }
    }

    private void loadJarByteCodes(InputStream is, String jarName) throws IOException {
        subJarNameList.add(jarName);

        JarInputStream jis = null;
        try {
            jis = new JarInputStream(is);
            Manifest manifest = jis.getManifest();
            for (JarEntry e = jis.getNextJarEntry(); null != e; e = jis.getNextJarEntry()) {
                if (e.isDirectory()) {
                    continue;
                }
                String entryName = e.getName();
                this.loadSingleByteCodes(jis, entryName, manifest, jarName);
            }
        } finally {
            if (null != jis) {
                jis.close();
            }
        }
    }

    private void loadSingleByteCodes(InputStream is, String entryName, Manifest manifest, String jarName) throws IOException {
        byte[] bytes = getBytes(is);
        if (isClass(entryName)) {
            String classBinaryName = resolveClassName(entryName);
            byteCodeCache.put(classBinaryName, new ByteCode(classBinaryName, entryName, bytes, manifest));
        }
        String resourceGlobalBinaryName = this.resolveResourceName(jarName, entryName);
        byteCodeCache.put(resourceGlobalBinaryName, new ByteCode(resourceGlobalBinaryName, entryName, bytes, manifest));
        byteCodeCache.put(entryName, new ByteCode(entryName, entryName, bytes, manifest));
    }

    private byte[] getBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IoUtil.copy(is, baos);
        return baos.toByteArray();
    }

    private boolean isJar(String name) {
        return name.startsWith(LIB_PREFIX) && name.endsWith(JAR_SUFFIX);
    }

    private boolean isClass(String name) {
        return name.endsWith(CLASS_SUFFIX);
    }

    private String resolveClassName(String name) {
        return name.substring(0, name.length() - 6).replace('/', '.');
    }

    private String resolveResourceName(String jarName, String resourceName) {
        return jarName + INNER_PREFIX_SEP + resourceName;
    }

    // ------------------ about search resource ------------------ //

    public List<String> searchResources(String resourcePath) {
        return this.searchResources(resourcePath, true);
    }

    public List<String> searchResources(String resourcePath, boolean isIncludeLib) {
        return this.searchResources(resourcePath, new ResourceFileter() {}, isIncludeLib);
    }

    public List<String> searchResources(String resourcePath, ResourceFileter resourceFileter) {
        return this.searchResources(resourcePath, resourceFileter, true);
    }

    public List<String> searchResources(String resourcePath, ResourceFileter resourceFileter, boolean isIncludeLib) {
        Preconditions.checkArgument(StringUtils.isNotBlank(resourcePath));
        Preconditions.checkNotNull(resourceFileter);
        return this.doSearchResources(resourcePath, resourceFileter, isIncludeLib);
    }

    private List<String> doSearchResources(String resourcePath, ResourceFileter resourceFileter, boolean isIncludeLib) {
        Set<String> set = Sets.newHashSet();
        Pattern pattern = generateResourceMatchPattern(resourcePath, isIncludeLib);
        for (Map.Entry<String, ByteCode> entry : byteCodeCache.entrySet()) {
            if (entry.getValue().isJavaClass()) {
                continue;
            }
            String key = entry.getKey();
            Matcher matcher = pattern.matcher(key);
            if (!matcher.matches()) {
                continue;
            }
            if (!isIncludeLib) {
                key = key.substring(MAIN_RESOURCE_PREFIX_SEP_LEN);
            } else {
                if (key.startsWith(MAIN_RESOURCE_PREFIX)) {
                    key = key.substring(MAIN_RESOURCE_PREFIX_SEP_LEN);
                }
            }
            if (resourceFileter.accept(key)) {
                set.add(key);
            }
        }
        return Lists.newArrayList(set);
    }

    private Pattern generateResourceMatchPattern(String source, boolean isIncludeLib) {
        String regex = "";
        if ("/".equals(source)) {
            // search currently dir ?
        } else if ("/*".equals(source)) {
            // search all resource ?
        } else if (source.endsWith("/*")) {
            source = source.substring(0, source.length() - 1);
            regex = source + "([a-zA-Z]{1}[a-zA-Z0-9]*/)*([a-zA-Z\\-\\_\\!]{1}[a-zA-Z0-9\\.\\-\\_\\!]*){1}";
        } else if (source.endsWith("/")) {
            regex = source + "([a-zA-Z\\-\\_\\!]{1}[a-zA-Z0-9\\.\\-\\_\\!]+){1}";
        } else {
            regex = source + "(/[a-zA-Z\\-\\_\\!]{1}[a-zA-Z0-9\\.\\-\\_\\!]+){1}";
        }

        if (!isIncludeLib) {
            regex = MAIN_RESOURCE_PREFIX + INNER_PREFIX_SEP + regex;
        } else {
            regex = "(?!" + MAIN_RESOURCE_PREFIX + "\\" + INNER_PREFIX_SEP + ")" + regex;
        }
        return Pattern.compile(regex);
    }

    @Override
    protected URL findResource(String name) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        name = this.resolveQueryResourceName(name);
        URL url = getParent().getResource(name);
        if (null != url) {
            return url;
        }
        try {
            return new URL(null, PluginResourceURLStreamHandler.getProtocol() + name, new PluginResourceURLStreamHandler(this));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unable to locate " + name, e);
        }
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        name = resolveQueryResourceName(name);
        final List<URL> urls = Lists.newArrayList();
        for (String jarName : subJarNameList) {
            String resourceGlobalBinaryName = resolveResourceName(jarName, name);
            ByteCode byteCode = byteCodeCache.get(resourceGlobalBinaryName);
            if (null != byteCode) {
                urls.add(new URL(null, PluginResourceURLStreamHandler.getProtocol() + resourceGlobalBinaryName,
                        new PluginResourceURLStreamHandler(this)));
            }
        }

        if (urls.isEmpty()) {
            return super.findResources(name);
        } else {
            return new Enumeration<URL>() {

                private int index = 0;
                @Override
                public boolean hasMoreElements() {
                    return index < urls.size();
                }

                @Override
                public URL nextElement() {
                    return urls.get(index++);
                }
            };
        }
    }

    private String resolveQueryResourceName(String name) {
        name = name.replaceAll("\\\\", "/");
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        String[] items = name.split("/");
        List<String> filteRet = new ArrayList<>();
        for (int i = 0; i < items.length; i++) {
            String item = items[i];
            if ((".").equals(item)) {
                continue;
            }
            if (("..").equals(item)) {
                i++;
            }
            filteRet.add(item);
        }
        StringBuilder retSb = new StringBuilder(64);
        for (String item : filteRet) {
            retSb.append(item).append("/");
        }
        return retSb.substring(0, retSb.length() - 1);
    }

    // ------------------ about search class ------------------ //

    private static final Pattern CLASS_DOT = Pattern.compile("([A-Z]{1}[a-zA-Z0-9]*){1}");
    private static final Pattern CLASS_DOT_STAR = Pattern.compile("([a-zA-Z]{1}[a-zA-Z0-9]*\\.)*([A-Z]{1}[a-zA-Z0-9]*){1}");

    public List<Class<?>> searchClasses(String pkgPath) {
        return this.searchClasses(pkgPath, new ClassFilter() {});
    }

    public List<Class<?>> searchClasses(String pkgPath, ClassFilter classFilter) {
        Preconditions.checkArgument(StringUtils.isNotBlank(pkgPath));
        Preconditions.checkNotNull(classFilter);
        pkgPath = pkgPath.trim();
        List<Class<?>> result = Lists.newArrayList();
        Pattern pattern = generateClassMatchPattern(pkgPath);
        for (Map.Entry<String, ByteCode> entry : byteCodeCache.entrySet()) {
            if (!entry.getValue().isJavaClass()) {
                continue;
            }
            String key = entry.getKey();
            Matcher matcher = pattern.matcher(key);
            if (!matcher.matches()) {
                continue;
            }
            try {
                Class<?> clazz = this.loadClass(key);
                if (null == clazz) {
                    continue;
                }
                if (classFilter.accept(clazz)) {
                    result.add(clazz);
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Cannot load class: " + key, e);
            }
        }
        return result;
    }

    private Pattern generateClassMatchPattern(String source) {
        if(".".equals(source)){
            return CLASS_DOT;
        }else if(".*".equals(source)){
            return CLASS_DOT_STAR;
        }else if (source.endsWith(".*")) {
            source = source.substring(0, source.length() - 2);
            source = source.replaceAll("\\.", "\\\\.");
            return Pattern.compile(source + "(\\.[a-zA-Z]{1}[a-zA-Z0-9]*)*(\\.[A-Z]{1}[a-zA-Z0-9]*){1}");
        } else {
            source = source.replaceAll("\\.", "\\\\.");
            return Pattern.compile(source + "(\\.[A-Z]{1}[a-zA-Z0-9]*){1}");
        }
    }

    // ------------------ about override ------------------ //


    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> clazz = super.findLoadedClass(name);
        if (null != clazz) {
            return clazz;
        }
        ByteCode byteCode = this.byteCodeCache.get(name);
        if (null == byteCode) {
            throw new ClassNotFoundException(name);
        }
        return this.defineClass(byteCode);
    }

    private Class<?> defineClass(ByteCode byteCode) {
        String name = byteCode.getBinaryName();
        int index = name.lastIndexOf('.');
        if (index != -1) {
            String pkgName = name.substring(0, index);
            Package pkg = super.getPackage(pkgName);
            Manifest manifest = byteCode.getManifest();
            if (null != pkg) {
                // TODO verify package if it is sealed.
            } else {
                if (null != manifest) {
                    this.definePackage(pkgName, manifest, protectionDomain.getCodeSource().getLocation());
                } else {
                    super.definePackage(pkgName, null, null, null, null, null, null,
                            protectionDomain.getCodeSource().getLocation());
                }
            }
        }
        return this.defineClass(byteCode.getBinaryName(), byteCode.getBytes(), protectionDomain);
    }

    private Class<?> defineClass(String name, byte[] bytes, ProtectionDomain protectionDomain) {
        return super.defineClass(name, bytes, 0, bytes.length, protectionDomain);
    }

    private Package definePackage(String name, Manifest man, URL url) throws IllegalArgumentException {
        String path = name.concat("/");
        String specTitle = null, specVersion = null, specVendor = null;
        String implTitle = null, implVersion = null, implVendor = null;
        String sealed = null;
        URL sealBase = null;

        Attributes attr = man.getAttributes(path);
        if (attr != null) {
            specTitle = attr.getValue(Attributes.Name.SPECIFICATION_TITLE);
            specVersion = attr.getValue(Attributes.Name.SPECIFICATION_VERSION);
            specVendor = attr.getValue(Attributes.Name.SPECIFICATION_VENDOR);
            implTitle = attr.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
            implVersion = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            implVendor = attr.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
            sealed = attr.getValue(Attributes.Name.SEALED);
        }
        attr = man.getMainAttributes();
        if (attr != null) {
            if (specTitle == null) {
                specTitle = attr.getValue(Attributes.Name.SPECIFICATION_TITLE);
            }
            if (specVersion == null) {
                specVersion = attr.getValue(Attributes.Name.SPECIFICATION_VERSION);
            }
            if (specVendor == null) {
                specVendor = attr.getValue(Attributes.Name.SPECIFICATION_VENDOR);
            }
            if (implTitle == null) {
                implTitle = attr.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
            }
            if (implVersion == null) {
                implVersion = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            }
            if (implVendor == null) {
                implVendor = attr.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
            }
            if (sealed == null) {
                sealed = attr.getValue(Attributes.Name.SEALED);
            }
        }
        if (sealed != null) {
            boolean isSealed = Boolean.parseBoolean(sealed);
            if (isSealed) {
                sealBase = url;
            }
        }
        return super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
    }

    InputStream getByteInputStream(String resource) {
        InputStream is;
        ClassLoader parent = getParent();
        if (null != parent) {
            is = parent.getResourceAsStream(resource);
            if (null != is) {
                return is;
            }
        }
        ByteCode byteCode = byteCodeCache.get(resource);
        return (null == byteCode) ? null : new ByteArrayInputStream(byteCode.getBytes());
    }

    public String getPluginJarPath() {
        return pluginJarPath;
    }

    public synchronized ProtectionDomain getProtectionDomain() {
        return protectionDomain;
    }

    private static final class ByteCode {

        private static final byte[] CLASS_HEADER_MIGIC_NUMBER = {
                (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE
        };
        private final String binaryName;
        private final String originalName;
        private final byte[] bytes;
        private final Manifest manifest;

        public ByteCode(String binaryName, String originalName, byte[] bytes, Manifest manifest) {
            this.binaryName = binaryName;
            this.originalName = originalName;
            this.bytes = bytes;
            this.manifest = manifest;
        }

        public boolean isJavaClass() {
            return bytes.length > 4 && (bytes[0] == CLASS_HEADER_MIGIC_NUMBER[0] && bytes[1] == CLASS_HEADER_MIGIC_NUMBER[1] && bytes[2] == CLASS_HEADER_MIGIC_NUMBER[2] && bytes[3] == CLASS_HEADER_MIGIC_NUMBER[3]);
        }

        public String getBinaryName() {
            return binaryName;
        }

        public String getOriginalName() {
            return originalName;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public Manifest getManifest() {
            return manifest;
        }

        @Override
        public String toString() {
            return "ByteCode{" +
                    "binaryName='" + binaryName + '\'' +
                    ", originalName='" + originalName + '\'' +
                    ", bytes=" + Arrays.toString(bytes) +
                    ", manifest=" + manifest +
                    '}';
        }
    }
}
