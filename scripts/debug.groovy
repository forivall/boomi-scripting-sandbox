// Debug script that dumps the current Document and Process Properties to a document.


import com.boomi.document.scripting.DataContextImpl
import com.boomi.execution.ExecutionManager;

NEWLINE = System.getProperty("line.separator");
StringBuilder sb = new StringBuilder();

DataContextImpl context = dataContext

// Get all process properties
execMan = ExecutionManager.getCurrent()
println("current ${execMan}")
execProps = ExecutionManager.getCurrent().getProperties();
sb.append("PROCESS PROPERTIES: " + NEWLINE);
formatProps(sb, execProps);
sb.append(NEWLINE);
outputDocProps = new Properties();

for( int i = 0; i < context.getDataCount(); i++ ) {
  InputStream is = context.getStream(i);
  Properties props = context.getProperties(i);

  outputDocProps.putAll(props);
  sb.append("DOCUMENT " + i + " PROPERTIES: " + NEWLINE);

  formatProps(sb, props);
  sb.append(NEWLINE);
  sb.append("set-cookie header: " + props.getProperty("document.dynamic.userdefined.outheader_Set-Cookie"));
  sb.append(NEWLINE);
}

// Output single document with props
is = new ByteArrayInputStream(sb.toString().getBytes());
context.storeStream(is, outputDocProps);

def formatProps(sb, props) {
  for (k in props.keySet()) {
    sb.append(k + "=" + props.get(k) + NEWLINE);
  }
}
