#!/usr/bin/env groovy -cp "vendor/boomi/*"
import com.boomi.connector.api.Payload
import com.boomi.connector.api.PayloadUtil
import com.boomi.container.config.AccountConfig
import com.boomi.container.config.ContainerConfig
import com.boomi.document.api.InboundDocument
import com.boomi.document.scripting.DataContextImpl as BoomiDataContextImpl
import com.boomi.store.BaseData
import com.forivall.boomi.DataContextImpl
import groovy.transform.Field
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer

import java.util.logging.ConsoleHandler
import java.util.logging.Logger

def __dirname = new File((String)(getClass().protectionDomain.codeSource.location.path)).parent

def cli = new CliBuilder(usage: 'groovy-script-runner -[e] [-p <processProps>] <script> [files] ')
// Create the list of options.
cli.h longOpt: 'help', 'show this message'
cli.e longOpt: 'empty', 'generate a single entry input file'
cli.p longOpt: 'props', args: 1, 'define process properties file'
cli.o longOpt: 'output', args: 1, 'define output file pattern'
opts = cli.parse(args)
if (opts.h) {
  cli.usage()
  return
}
def outputArg = opts.o
def hasOutputFile = (Boolean)outputArg
String outputFileName = outputArg instanceof String ? outputArg : null
def args = opts.arguments()
def (scriptFile, fileNames) = [args.head(), args.tail()]
if (!scriptFile) {
  cli.usage()
  System.exit(1)
  return
}
assert scriptFile instanceof String

if (fileNames.size() > 0 && opts.e) {
  println('cannot specify empty input and input files at the same time')
  cli.usage()
  System.exit(1)
  return
}

def logger = Logger.getLogger('script-runner')

class EmptyDocument implements InboundDocument {
  InputStream _is
  Properties _p
  EmptyDocument(String propertiesFile) {
    EmptyDocument()
    _p.load(new FileInputStream(propertiesFile))
  }
  EmptyDocument() {
    _is = new ByteArrayInputStream(new byte[0])
    _p = new Properties()
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
  FileDocument(String fileName) {
    _fileName = fileName
    _is = new FileInputStream(fileName)
    _p = new Properties()
    try {
      _p.load(new FileInputStream(fileName + '.properties'))
    } catch (FileNotFoundException) {}
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

class DataContext implements DataContextImpl {
  static interface DataContextOutput {
    OutputStream get(int index)
  }
  static class FileOutput implements DataContextOutput {
    String outPath = 'output%d.json'
    OutputStream get(int index) {
      return new FileOutputStream(String.format(outPath, index), true)
    }
  }
  static class PipeOutput implements DataContextOutput {
    OutputStream dest = System.out
    OutputStream get(int index) {
      return dest
    }
  }
  private static systemOut = new PipeOutput(dest: System.out)
  protected static logger = Logger.getLogger('DataContext')
  private boolean used;
  private List<InboundDocument> _documents
  private Integer storedCount = 0
  private DataContextOutput output

  DataContext(List<InboundDocument> documents) {
    this(documents, systemOut)
  }
  DataContext(List<InboundDocument> documents, String outPath) {
    this(documents, new FileOutput(outPath: outPath))
  }
  DataContext(List<InboundDocument> documents, DataContextOutput output) {
    _documents = documents
    this.output = output
  }

  public void setCombineAll(boolean combineAll) { /* void */ }

  public List<Properties> getMetaDataList() {
    return new BoomiDataContextImpl.MetaDataList();
  }

  public List<InputStream> getStreamList() {
    return new BoomiDataContextImpl.StreamList();
  }

  public boolean isUsed() {
    return this.used;
  }

  public int getDataCount() {
    this.used = true;
    return this._documents.size()
  }

  public BaseData getData(int index) {
    throw new Error('Not Implemented')
  }

  public InputStream getStream(int index) {
    logger.info('getStream ' + index)
    logger.info('' + this._documents.get(index))
    return this._documents.get(index).getInputStream()
  }

  public Properties getProperties(int index) {
    this.used = true;
    def d = this._documents.get(index)
    def p = d.getProperties()
    logger.info("getProperties ${index} ${p} ${d}")
    return p
  }

  public void storeStream(InputStream stream, Properties properties) throws Exception {
    def payload = PayloadUtil.toPayload(stream)
    this.storePayload(payload, properties);
  }

  public void storePayload(Payload payload, Properties properties) throws Exception {
    println("Payload ${storedCount++}")
    properties.list(System.out)
    println('-- end properties --')

    def output = this.output.get(storedCount)
    logger.info(String.format('%s', output.toString()))

    println('-- dumping payload --')
    def input = payload.readFrom()
    int size = 0;
    byte[] buffer = new byte[1024];
    while ((size = input.read(buffer)) != -1) output.write(buffer, 0, size);

    println('\n-- end payload --')
  }

  void addUnusedDocuments() {
    println('addUnusedDocuments')
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

def documents = opts.e ? [opts.p ? new EmptyDocument(opts.p) : new EmptyDocument()] : fileNames.collect { new FileDocument(it) }

if (hasOutputFile) {
  logger.info(String.format('outputFileName %s', outputFileName))
}
def context = hasOutputFile ?
  new DataContext((List<InboundDocument>)documents, outputFileName) :
  new DataContext((List<InboundDocument>)documents);

// TODO: // Add imports for script.
// def importCustomizer = new ImportCustomizer()

def configuration = new CompilerConfiguration()
//configuration.addCompilationCustomizers(importCustomizer)
def sd = new groovy.lang.Binding()
def sh = new GroovyShell(sd, configuration)

sd.setProperty('dataContext', context)

class ExecutionManager {
  static def singleton = new ExecutionManager()
  def _props = new Properties()
  public getProperties() {
    return _props
  }
  public static getCurrent() {
    return singleton
  }
}
class ExecutionUtil {
  static Logger _logger = Logger.getLogger('script-runner')
  public static getBaseLogger() {
    return _logger
  }
  public static getDynamicProcessProperty(String key) {
    return ExecutionManager.getCurrent().getProperties().getProperty(key)
  }
  public static void setDynamicProcessProperty(String key, String value, boolean persist) {
    if (persist) _logger.info("Will persist DPP ${key}")
    if (value != null) ExecutionManager.getCurrent().getProperties().setProperty(key, value);
  }
}

if (opts.p) {
  String propsFile = opts.p
  ExecutionManager.getCurrent().getProperties().load(new FileInputStream(propsFile))
}

// TODO: load props from file into the mock execution manager's props

sd.setProperty('ExecutionManager', ExecutionManager)
sd.setProperty('ExecutionUtil', ExecutionUtil)
sd.setProperty('DataContextImpl', DataContext)

def replacements = [
  'import com.boomi.execution.ExecutionManager': '',

  'import com.boomi.execution.ExecutionUtil': '',

  'import com.boomi.document.scripting.DataContextImpl':
    'import com.forivall.boomi.DataContextImpl',

  'DataContextImpl context = dataContext':
    'def context = dataContext'
]
def f = new File( scriptFile )
def lastSep = scriptFile.lastIndexOf(File.separator)
def basename = scriptFile.substring(lastSep + File.separator.length())
def fakeBasename = basename.replace('-', '_')
// modify the file so that it uses our custom ExecutionManager
String modifiedSource = f.readLines()
  .collect({
  for (entry in replacements.entrySet()) {
    if (it.startsWith(entry.key)) {
      return it.replace(entry.key, entry.value)
    }
  }
  return it
}).join('\n')
def code = new GroovyCodeSource(modifiedSource,
  scriptFile.substring(0, lastSep) + File.separator + fakeBasename,
  "/groovy/shell"
)

// see

import com.boomi.document.scripting.ScriptingDocumentHandler

def script = sh.parse(code)

try {
  script.run()
} catch (Exception e) {
  def os = new ByteArrayOutputStream()
  def ps = new PrintStream(os, true, 'utf-8')
  e.printStackTrace(ps)
  def s = os.toString('utf-8')
  logger.severe(s.replace(fakeBasename, basename))
}
