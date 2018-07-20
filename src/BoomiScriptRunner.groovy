#!/usr/bin/env groovy -cp "vendor/boomi/*"
import com.boomi.container.config.AccountConfig
import com.boomi.container.config.ContainerConfig
import com.boomi.document.api.InboundDocument

import com.forivall.boomi.DataContextImpl
import com.forivall.boomi.ExecutionManager
import com.forivall.util.ResettableFileInputStream
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.runtime.InvokerHelper

//import java.security.AccessController
//import java.security.PrivilegedAction
import java.util.logging.Level
import java.util.logging.Logger

def __dirname = new File((String)(getClass().protectionDomain.codeSource.location.path)).parent

def cli = new CliBuilder(usage: 'groovy-script-runner -[e] [-p <processProps>] <script> [files] ')
// Create the list of options.
cli.options.addOption cli.option('h', [longOpt: 'help'], 'show this message')
cli.options.addOption cli.option('e', [longOpt: 'empty'], 'generate a single entry input file')
cli.options.addOption cli.option('p', [longOpt: 'props', args: 1], 'define process properties file')
cli.options.addOption cli.option('d', [longOpt: 'docprops', args: 1], 'define document properties file')
cli.options.addOption cli.option('o', [longOpt: 'output', args: 1], 'define output file pattern')
OptionAccessor opts = cli.parse(args)

if (opts == null) return
if (opts.getProperty('help')) {
  cli.usage()

  return
}

def outputArg = opts.getProperty('output')
def hasOutputFile = (Boolean)outputArg
String outputFileName = outputArg instanceof String ? outputArg : null
def args = opts.arguments()
if (args.size() == 0) {
  System.err.println('At least 1 argument required\n')
  cli.usage()
  System.exit(1)

  return
}
def scriptFile = args.head()
def fileNames = args.tail()
if (!scriptFile) {
  cli.usage()
  System.exit(1)
  return
}
assert scriptFile instanceof String

if (fileNames.size() > 0 && opts.getProperty('empty')) {
  println('cannot specify empty input and input files at the same time')
  cli.usage()
  System.exit(1)
  return
}

def logger = Logger.getGlobal()
def p = logger
while (p != null) {
  p.level = Level.ALL
  p.handlers.each {h ->
    h.level = Level.ALL
  }
  if (!p.useParentHandlers) break
  p = p.parent
}

class EmptyDocument implements InboundDocument {
  InputStream _is
  Properties _p
  EmptyDocument() {
    _is = new ByteArrayInputStream(new byte[0])
    _p = new Properties()
  }
  EmptyDocument(String propertiesFile) {
    this()
    _p.load(new FileInputStream(propertiesFile))
  }
  EmptyDocument(List<String> propertiesFiles) {
    this()
    def props = _p
    def logger = Logger.getGlobal()
    propertiesFiles.each { String propertiesFile ->
      logger.fine("loading ${propertiesFile}...")
      props.load(new FileInputStream(propertiesFile))
    }
  }
  InputStream getInputStream() {
    return _is
  }
  long getSize() {
    return 0
  }
  Logger getLogger() {
    return logger
  }
  String getProperty(String propertyName) {
    return _p.getProperty(propertyName)
  }
  Properties getProperties() {
    println('getProperties')
    println(_p)
    return _p
  }
}


class FileDocument implements InboundDocument {
  InputStream _is
  Properties _p
  String _fileName
  FileDocument(String fileName, List<String> propertiesFiles = []) {
    _fileName = fileName
    _is = new ResettableFileInputStream(fileName)
    _p = new Properties()
    try {
      _p.load(new FileInputStream(fileName + '.properties'))
    } catch (FileNotFoundException) {}

    def props = _p
    def logger = Logger.getGlobal()
    propertiesFiles.each { String propertiesFile ->
      logger.fine("loading ${propertiesFile}...")
      props.load(new FileInputStream(propertiesFile))
    }
  }

  InputStream getInputStream() {
    return _is
  }

  long getSize() {
    return 0
  }

  Logger getLogger() {
    return Logger.getLogger('Document ' + _fileName)
  }
  String getProperty(String propertyName) {
    return _p.getProperty(propertyName)
  }
  Properties getProperties() {
    return _p
  }
}

System.setProperty("com.boomi.container.libDir", "./lib");
System.setProperty("com.boomi.container.runAsAccount", "")
System.setProperty("com.boomi.container.account", "TEST")
System.setProperty("com.boomi.container.accountDir", "/Users/emilyklassen/Code/mcs-repos/monorepo/sandboxes/ltc/groovy/accDir")
def cc = ContainerConfig.createTest()
def ac = AccountConfig.create(cc, 'TEST')
def coll = Collections.singleton('TEST')
//ExecutionManager.createCurrentExecution(ac, 'TEST', '.', {})
//println("currentExecution ${ExecutionManager.getCurrent()}")

List<InboundDocument> documents

if (opts.getProperty('docpropss')) {
  List<String> propsFiles = opts.getProperty('docpropss')
  if (opts.getProperty('empty')) {
    documents = [new EmptyDocument(propsFiles)]
  } else {
    documents = fileNames.collect { String fileName -> new FileDocument(fileName, propsFiles) }
  }
} else {
  if (opts.getProperty('empty')) {
    documents = [new EmptyDocument()]
  } else {
    documents = fileNames.collect { String fileName -> new FileDocument(fileName) }
  }
}

if (hasOutputFile) {
  logger.info(String.format('outputFileName %s', outputFileName))
}
def context = hasOutputFile ?
  new DataContextImpl(documents, outputFileName) :
  new DataContextImpl(documents);

// TODO: // Add imports for script.
// def importCustomizer = new ImportCustomizer()

def configuration = new CompilerConfiguration()
//configuration.addCompilationCustomizers(importCustomizer)
def sd = new groovy.lang.Binding()

sd.setProperty('dataContext', context)

if (opts.getProperty('propss')) {
  List<String> propsFiles = opts.getProperty('propss')
  def props = ExecutionManager.current.getProperties()
  propsFiles.each { String propsFile ->
    props.load(new FileInputStream(propsFile))
  }
}

// TODO: load props from file into the mock execution manager's props

def replacements = [
  'import com.boomi.execution.ExecutionManager':
    'import com.forivall.boomi.ExecutionManager',

  'import com.boomi.execution.ExecutionUtil':
    'import com.forivall.boomi.ExecutionUtil',

  'import com.boomi.document.scripting.DataContextImpl':
    'import com.forivall.boomi.DataContextImpl',
]
def f = new File( scriptFile )
def lastSep = scriptFile.lastIndexOf(File.separator)
def basename = scriptFile.substring(lastSep + File.separator.length())
def fakeBasename = basename.replace('-', '_')

if ((~/^[0-9]/).matcher(fakeBasename).lookingAt()) {
  fakeBasename = '_' + fakeBasename
}

// modify the file so that it uses our custom ExecutionManager
String modifiedSource = f.readLines()
  .collect({ String it ->
  for (entry in replacements.entrySet()) {
    if (it.startsWith(entry.key)) {
      return it.replace(entry.key, entry.value)
    }
  }
  return it
}).join('\n')
def scriptFileName = scriptFile.substring(0, lastSep) + File.separator + fakeBasename;

def code = new GroovyCodeSource(modifiedSource, (String) scriptFileName, "/groovy/shell")
//GroovyCodeSource code = AccessController.doPrivileged( new PrivilegedAction<GroovyCodeSource>() {
//  public GroovyCodeSource run() {
//    // 2.6
//    // new GroovyCodeSource(modifiedSource, (String) scriptFileName, GroovyShell.DEFAULT_CODE_BASE)
//    new GroovyCodeSource(modifiedSource, (String) scriptFileName, "/groovy/shell")
//  }
//})
//

def classLoader = this.class.getClassLoader()
def loader = new GroovyClassLoader( classLoader, CompilerConfiguration.DEFAULT )
//GroovyClassLoader loader = AccessController.doPrivileged( new PrivilegedAction<GroovyClassLoader>() {
//  public GroovyClassLoader run() {
//    return new GroovyClassLoader( classLoader, CompilerConfiguration.DEFAULT );
//  }
//} );

// see
def scriptClass = loader.parseClass(code)

try {
  Script script = InvokerHelper.createScript(scriptClass, sd);
  script.run()
} catch (Exception e) {
  def os = new ByteArrayOutputStream()
  def ps = new PrintStream(os, true, 'utf-8')
  e.printStackTrace(ps)
  def s = os.toString('utf-8')
  logger.severe(s.replace(fakeBasename, basename))
}

ExecutionManager.singletonTask.dumpDirtyProperties(System.out)
