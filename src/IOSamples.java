import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.*;
import javax.tools.JavaCompiler.*;

public class IOSamples {
	
	
	ArrayList<ArrayList<String>> inputList = new ArrayList<ArrayList<String>>();
	ArrayList<ArrayList<String>> outputList = new ArrayList<ArrayList<String>>();
	
	IOSamples() {		
	}
	
	/*
	 * we assume that each output line contains at least one token of length >=1
	 */
	public static IOSamples parseFile(String path) throws IOException {
		IOSamples io = new IOSamples();
		
		BufferedReader br = new BufferedReader(new FileReader(new File(path)));
		String line;
		int stage = 0;	// 0: input, 1: output
		
		while((line=br.readLine())!=null) {	
			line = line.trim();
			if (line.length() == 0) continue;	// skip empty lines
			
			if (line.toLowerCase().equals("\\null")) {	// no output exists
				if (stage == 0) {
					System.err.println("\\null must be used for output, not for input");
					System.exit(0);
				}
				line = "";
			} 
			
			ArrayList<String> al = new ArrayList<String>();
			String[] tokens = line.split(" ");
			for(String token:tokens) {
				if (token.length() == 0) continue;	// empty space
				al.add(token);
			}
			switch(stage) {
			case 0: io.inputList.add(al); break;
			case 1: io.outputList.add(al); break;
			}
			stage++;
			stage = stage % 2;
		}
		br.close();
		
		// sanity check: # of input lines == # of output lines?
		System.out.println("# of input/output lines(" + io.inputList.size() + "," 
				+ io.outputList.size() + ")");
		if (io.inputList.size() != io.outputList.size()) {
			System.err.println("# of input/output lines do not match");
			System.exit(0);
		}
		
		return io;
	}
	
	/*
	 * return true, if code generates correct outputs for all inputs
	 * return false, otherwise
	 * 
	 * code: code to validate
	 */
	public Result validateCode(String code) throws IOException, InterruptedException {
		// find the class name in the code, as this should be used as a file name
		String className = findClassName(code);
		if (className == null) {
			System.err.println("a valid class name is not found in the following code");
			System.err.println(code);
			System.exit(0);
		}
		
		// write the code into a file
		String homeDirectory = System.getProperty("user.home");		
		BufferedWriter bw = new BufferedWriter(new FileWriter(homeDirectory + "/" + className + ".java"));
		bw.write(code);
		bw.close();		
		
		// compile the code to generate an executable
		ProcessBuilder pb = new ProcessBuilder();		
		pb.command("javac", className + ".java").directory(new File(homeDirectory + "/")); // make sure that each parameter is input as a separate parameter		
		Process process = pb.start();
		int exitCode = process.waitFor();
		if (exitCode != 0) {
			System.err.println("compilation failed.");
			System.exit(0);
		}
		// System.out.println("exit code (compilation):" + exitCode);
		
		//printResults(process);
		
		// run the executable with input samples
		//	and validate generated outputs with output samples
		Result result = new Result();
		for(int i=0;i<inputList.size();i++) {
			ArrayList<String> inputs = inputList.get(i);
			ArrayList<String> outputs = outputList.get(i);
			
			inputs.add(0, "java");	// either use 'javaw' or 'java'
			inputs.add(1, className);			
			pb = new ProcessBuilder();
			pb.command(inputs).directory(new File(homeDirectory + "/"));
			process = pb.start();
			exitCode = process.waitFor();
			//printResults(process);
			if (compareResults(process, outputs)) result.numSuccess++;
			else result.numFailure++;
			if (exitCode != 0) {
				System.out.println("exit code (validation):" + exitCode);
				break;
			}
			
			inputs.remove(0);
			inputs.remove(0);
		}				
		
		return result;
	}
	
	/* 
	 * compile within memory, rather than doing it on disk
	 */
	public Result validateCode2(String code) throws IOException, InterruptedException {
		// find the class name in the code, as this should be used as a file name
		String className = findClassName(code);
		if (className == null) {
			System.err.println("a valid class name is not found in the following code");
			System.err.println(code);
			System.exit(0);
		}
		
		// write the code into a file
		
		//String homeDirectory = System.getProperty("user.home");		
		/*
		BufferedWriter bw = new BufferedWriter(new FileWriter(homeDirectory + "/" + className + ".java"));
		bw.write(code);
		bw.close();		
		*/
		// compile the code to generate an executable
		
		/*
		ProcessBuilder pb = new ProcessBuilder();
		pb.command("javac", className + ".java").directory(new File(homeDirectory + "/")); // make sure that each parameter is input as a separate parameter		
		Process process = pb.start();
		int exitCode = process.waitFor();
		if (exitCode != 0) {
			System.err.println("compilation failed.");
			System.exit(0);
		}
		// System.out.println("exit code (compilation):" + exitCode);
		*/
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<JavaFileObject>();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(collector, null, null);
		
		// prepare a compilation target in a String object
		JavaFileObject javaObjectFromString = null;
		try {
			javaObjectFromString = new JavaObjectFromString(className, code);
		} catch (URISyntaxException e) {			
			e.printStackTrace();
		}		
		Iterable<? extends JavaFileObject> fileObjects = Arrays.asList(javaObjectFromString);
		
		Writer output = null;
		try {
			output = new FileWriter(className + ".class");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CompilationTask task = compiler.getTask(output, fileManager, collector, null, null, fileObjects);
		Boolean compilationResult = task.call();
		output.close();
		
		List<Diagnostic<? extends JavaFileObject>> diagnostics = collector.getDiagnostics();
		for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
			JavaFileObject source = diagnostic.getSource();
			if (source != null) {
				System.out.print(diagnostic.getKind() + " in ");	// error or warning
				System.out.println(source);
				System.out.println("line #" + diagnostic.getLineNumber() + " column #" + diagnostic.getColumnNumber());				
				System.out.println(diagnostic.getMessage(Locale.ENGLISH));
				System.out.println();
			}
		}
		
		if (!compilationResult) {
			System.err.println("compilation failed.");
			System.exit(0);
		}
		
		//printResults(process);
		
		// run the executable with input samples
		//	and validate generated outputs with output samples
		Result result = new Result();
		for(int i=0;i<inputList.size();i++) {
			ArrayList<String> inputs = inputList.get(i);
			ArrayList<String> outputs = outputList.get(i);
			
			inputs.add(0, "java");	// either use 'javaw' or 'java'
			inputs.add(1, className);			
			ProcessBuilder pb = new ProcessBuilder();
			pb.command(inputs);//.directory(new File(homeDirectory + "/"));
			Process process = pb.start();
			int exitCode = process.waitFor();
			//printResults(process);
			if (compareResults(process, outputs)) result.numSuccess++;
			else result.numFailure++;
			if (exitCode != 0) {
				System.out.println("exit code (validation):" + exitCode);
				break;
			}
			
			inputs.remove(0);
			inputs.remove(0);
		}				
		
		return result;
	}
	
	static class Result {
		public int numSuccess = 0;
		public int numFailure = 0;
		Result() {}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("# of success/failure: " + numSuccess + "," + numFailure);			
			return sb.toString();
		}
	}
	
	static Pattern patternClassName = Pattern.compile("class\\s+\\w+\\s*\\{");
	public static String findClassName(String code) {
		Matcher matcher = patternClassName.matcher(code);
		if (matcher.find()) {
			return code.substring(matcher.start()+5,matcher.end()-1).trim();
		}
		return null;
	}
	
	public static void printResults(Process process) throws IOException {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	    String line = "";
	    while ((line = reader.readLine()) != null) {
	        System.out.println(line);
	    }
	    reader.close();
	}
	
	public static String returnResults(Process process) throws IOException {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	    StringBuilder sb = new StringBuilder();
	    String line = "";
	    while ((line = reader.readLine()) != null) {	        
	        sb.append(line);
	    }
	    reader.close();
	    return sb.toString();
	}
	
	public static boolean compareResults(Process process, ArrayList<String> outputs) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	    StringBuilder sb = new StringBuilder();
	    String line = "";
	    while ((line = reader.readLine()) != null) {	        
	        sb.append(line);
	    }
	    reader.close();
	    
	    String[] tokens = sb.toString().split(" ");
	    int outputIndex = 0;
	    for(int i=0;i<tokens.length;i++) {
	    	String token = tokens[i].trim();
	    	if (token.length() == 0) continue;
	    	
	    	if (outputIndex >= outputs.size()) return false;	// more output tokens produced than necessary
	    	if (!outputs.get(outputIndex).equals(token)) return false;
	    	outputIndex++;
	    }
	    if (outputIndex < outputs.size()) return false;	// fewer output tokens produced than necessary
	    
	    return true;
	}
	
	static class JavaObjectFromString extends SimpleJavaFileObject {
		private String contents = null;
		protected JavaObjectFromString(String classname, String contents) throws URISyntaxException {
			super(new URI(classname + ".java"), Kind.SOURCE);
			this.contents = contents;
		}
		
		public CharSequence getCharContent(boolean ignoreEncodingErrors) {
			return contents;
		}
	}
}
