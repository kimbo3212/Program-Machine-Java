import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/*
 * expressionID: integer >= 0
 * 		code sketch has an expressionID 0,
 * 		and other expressions have it > 0
 */
public class Expression {
	static Pattern pattern = Pattern.compile("e\\d+");
	static Pattern patternDefinition = Pattern.compile("e\\d+:");
	
	static HashMap<Integer,Expression> expressions = new HashMap<Integer,Expression>();
	
	String expressionString = "";
	int expressionID = -1;
	ArrayList<Expression> expressionOred = new ArrayList<Expression>(); 
	ArrayList<Expression> expressionAnded = new ArrayList<Expression>();
		
	Expression() {
		
	}
	
	Expression(String expressionString) {
		this.expressionString = new String(expressionString); 
	}
	
	Expression(int expressionID) {
		this.expressionID = expressionID;
	}
	
	void and(String expressionString) {
		if (expressionString.length() > 0) {
			if (this.expressionAnded.size() > 0) {
				this.expressionAnded.add(new Expression(expressionString));
			} else if (this.expressionString.length() > 0) {
				this.expressionAnded.add(new Expression(this.expressionString));
				this.expressionString = "";
				this.expressionAnded.add(new Expression(expressionString));
			} else if (this.expressionID >= 0) {
				this.expressionAnded.add(new Expression(this.expressionID));
				this.expressionID = -1;
				this.expressionAnded.add(new Expression(expressionString));
			}
			else {			
				this.expressionString = new String(expressionString);
			} 
		}
	}
	void and(int expressionID) {
		if (expressionID >= 0) {
			if (this.expressionAnded.size() > 0) {
				this.expressionAnded.add(new Expression(expressionID));
			} else if (this.expressionString.length() > 0) {
				this.expressionAnded.add(new Expression(this.expressionString));
				this.expressionString = "";
				this.expressionAnded.add(new Expression(expressionID));
			} else if (this.expressionID >= 0) {
				this.expressionAnded.add(new Expression(this.expressionID));
				this.expressionID = -1;
				this.expressionAnded.add(new Expression(expressionID));
			}
			else {			
				this.expressionID = expressionID;
			} 
		}
	}
	
	void or(Expression e) {
		this.expressionOred.add(e);
	}
	
	Expression duplicate() {
		Expression e = new Expression();
		e.expressionString = this.expressionString;
		e.expressionID = this.expressionID;
		
		// re-create lists since their elements can be modified according to depthMax
		for (Expression ex:this.expressionAnded) {
			e.expressionAnded.add(ex.duplicate());
		}
		for(Expression ex:this.expressionOred) {
			e.expressionOred.add(ex.duplicate());
		}	
		
		return e;
	}
	
	static final int typeEmpty = -1;
	static final int typeString = 0;
	static final int typeExpressionID = 1;
	static final int typeAnded = 2;
	static final int typeOred = 3;	
	int type() {
		if (this.expressionString.length()>0) return typeString;
		else if (this.expressionID>=0) return typeExpressionID;
		else if (this.expressionAnded.size()>0) return typeAnded;
		else if (this.expressionOred.size()>0) return typeOred;
		else return typeEmpty;
	}
	
	boolean containExpression() {
		switch(this.type()) {		
		case typeExpressionID:
			return true;
		case typeAnded:
			for(Expression e:this.expressionAnded)
				if (e.containExpression()) return true;
			break;
		case typeOred:
			for(Expression e:this.expressionOred)
				if (e.containExpression()) return true;
		}
		return false;
	}
	
	static boolean isExpression(String token) {
		Matcher matcher = pattern.matcher(token);
		return matcher.find() && matcher.start()==0 && matcher.end()==token.length();
	}
	
	static int expressionID(String token) {
		return Integer.valueOf(token.substring(1, token.length()));		
	}
	
	static boolean isExpressionDefinition(String token) {
		Matcher matcher = patternDefinition.matcher(token);
		return matcher.find() && matcher.start()==0 && matcher.end()==token.length();
	}
	
	static int expressionDefinitionID(String token) {
		return Integer.valueOf(token.substring(1, token.length()-1));		
	}
	
	static Expression parseAndedExpression(String line) {
		Expression e = new Expression();
		StringBuilder codeSketch = new StringBuilder();
		String[] tokens = line.split(" ");
		for(String token:tokens) {			
			if (token.length() == 0) continue;	// empty space
			if (isExpression(token)) {				
				e.and(codeSketch.toString());							
				codeSketch.setLength(0);
				
				e.and(expressionID(token));
				//e.expressionAnded.add(new Expression(expressionID(token)));
				//System.out.println("**" + expressionNumber(token));
			} else {
				if (codeSketch.length() == 0)
					codeSketch.append(token);
				else
					codeSketch.append(" " + token);
			}				
		}
		e.and(codeSketch.toString());							
		codeSketch.setLength(0);
		
		return e;
	}
	
	public static void parseFile(String path) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(new File(path)));
		String line;
		StringBuilder codeSketch = new StringBuilder();
		int stage = 0;
		Expression e0 = new Expression();
		Expression eCurrent = null;
		int eID = -1;		
		
		while((line=br.readLine())!=null) {
			// remove comments that start with '//'
			int comment_index = line.indexOf("//");
			if (comment_index>=0) 
				line = line.substring(0,comment_index);
			
			line = line.trim();
			if (line.length() == 0) continue;	// empty line
			
			if (isExpressionDefinition(line)) {	// definition begins (e.g., "e1:")
				if (stage == 0) { // flush out all segments of e0 remaining in buffer
					e0.and(codeSketch.toString());							
					codeSketch.setLength(0);
				} else if (stage == 1) { // store previous definition in hashmap
					expressions.put(eID, eCurrent);
				}
				
				eCurrent = new Expression();
				eID = expressionDefinitionID(line);
				//System.out.println(eID);
				stage = 1;
				continue;
			}
			
			if (stage == 0) {	// parse e0				
				String[] tokens = line.split(" ");
				//System.out.println(Arrays.toString(tokens));
				for(String token:tokens) {
					if (token.length() == 0) continue;	// empty space
					if (isExpression(token)) {						
						e0.and(codeSketch.toString());							
						codeSketch.setLength(0);
							
						e0.and(expressionID(token));
						//e0.expressionAnded.add(new Expression(expressionID(token)));
						//System.out.println("**" + expressionNumber(token));
					} else {
						if (codeSketch.length() == 0)
							codeSketch.append(token);
						else
							codeSketch.append(" " + token);
					}				
				}
			} else if (stage == 1) {	// parse expressions other than e0 (i.e. e1 and after)				
				eCurrent.or(parseAndedExpression(line));
			}
		}
		br.close();
		
		if (eCurrent != null)	// store lastly defined expression
			expressions.put(eID, eCurrent);
		
		expressions.put(0, e0);	// store main expression e0
		
		// sanity check to see whether an expression is referred but not defined
		findMissingReferences();
	}
	
	// sanity check to see whether an expression is referred but not defined
	static HashMap<Integer,ArrayList<Integer>> IDtoDefinitions; // stores which expressions are referred within definition of which expressions
	static void findMissingReferences() {		
		IDtoDefinitions = new HashMap<Integer,ArrayList<Integer>>(); 
		
		for(Integer key:expressions.keySet()) {
			storeReferences(expressions.get(key), key);
		}		
		
		boolean missingReferences = false;
		for(Integer key:IDtoDefinitions.keySet()) {
			if (expressions.containsKey(key) == false) {
				System.err.println("e" + key + " is used in the following expressions but never defined");
				System.err.println(IDtoDefinitions.get(key).stream().map(Object::toString).collect(Collectors.joining(", ")));
				missingReferences = true;
			}
		}
		if (missingReferences)
			System.exit(0);
	}
	static void storeReferences(Expression e, int id) {
		if (e.expressionString.length() > 0) return;
		else if (e.expressionID >= 0) {
			if (IDtoDefinitions.containsKey(e.expressionID)) {
				ArrayList<Integer> definitions = IDtoDefinitions.get(e.expressionID);
				definitions.add(id);
			} else {
				ArrayList<Integer> definitions = new ArrayList<Integer>();
				definitions.add(id);
				IDtoDefinitions.put(e.expressionID, definitions);
			}
		}
		else if (e.expressionOred.size() > 0) {			
			for(int i=0;i<e.expressionOred.size();i++) {		
				storeReferences(e.expressionOred.get(i), id);				
			}			
		} else if (e.expressionAnded.size() > 0) {
			for(int i=0;i<e.expressionAnded.size();i++) {
				storeReferences(e.expressionAnded.get(i), id);				
			}
		} 
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		if (this.expressionString.length() > 0) sb.append(this.expressionString);
		else if (this.expressionID >= 0) sb.append("e" + Integer.toString(this.expressionID));
		else if (this.expressionOred.size() > 0) {			
			//sb.append("[");	sb.append(System.lineSeparator());
			for(int i=0;i<this.expressionOred.size();i++) {		
				sb.append(this.expressionOred.get(i).toString());
				if (i<this.expressionOred.size()-1) sb.append(System.lineSeparator());
			}
			//sb.append("]");			
		} else if (this.expressionAnded.size() > 0) {
			for(int i=0;i<this.expressionAnded.size();i++) {
				sb.append(this.expressionAnded.get(i).toString());
				if (i<this.expressionAnded.size()-1) sb.append(" ");
			}
		} else {
			sb.append("[warning] this expression is empty");
		}
		
		//return sb.toString().replace("{", "{" + System.lineSeparator()).replace("}", "}" + System.lineSeparator());
		return sb.toString();
	}
	
	// return string representation of all expressions
	public static String toStringAllExpressions() {
		StringBuilder sb = new StringBuilder();
		
		for(Integer key:Expression.expressions.keySet()) {
			sb.append("e" + key + ":");
			sb.append(System.lineSeparator());
			sb.append(expressions.get(key));			
			sb.append(System.lineSeparator());
			sb.append(System.lineSeparator());
		}
		
		return sb.toString();
	}
	
	/*
	 *  explore down to max depth
	 *  create an array, such that
	 *  each element in array represents current selection of each ORed expression  
	 */
	int selection, selectionMax;	
	Expression parent;
	
	static ArrayList<Expression> selections;
	static int distinctCodeCount;
	static int depthMax;	
	static Expression e0;
	static void initiateExpressionTree(int depthMax) {
		Expression.depthMax = depthMax;
		selections = new ArrayList<Expression>();
		e0 = expressions.get(0).duplicate();
		e0.initiateExpression(0);
		e0.parent = null;
		distinctCodeCount = 0;
	}
	void initiateExpression(int depth) {
		
		// single expression
		if (this.expressionID >= 0) {
			this.expressionAnded.add(new Expression(this.expressionID));
			this.expressionID = -1;
		}
		
		// ANDed expression
		HashMap<Integer,Expression> IDtoExpression = new HashMap<Integer,Expression>();
		for(int i=0; i<this.expressionAnded.size(); i++) {
			Expression e = this.expressionAnded.get(i);
			if (e.expressionID >= 0) {
				e = expressions.get(e.expressionID).duplicate();
				IDtoExpression.put(i,e); // postpone replacing elements to after iteration to prevent errors
				//this.expressionAnded.set(i, e);
				e.initiateExpression(depth+1);				
			}
			e.parent = this;
		}
		for(Integer id:IDtoExpression.keySet()) {
			this.expressionAnded.set(id, IDtoExpression.get(id));
		}
		
		// ORed expression
		IDtoExpression.clear();
		ArrayList<Integer> indicesToRemove = new ArrayList<Integer>();
		int numSelections = 0;
		for(int i=0; i<this.expressionOred.size(); i++) {
			Expression e = this.expressionOred.get(i);
			switch(e.type()) {
			case Expression.typeString:
				numSelections++;
				break;
			case Expression.typeExpressionID:				
				if (depth<depthMax) {
					numSelections++;
					e = expressions.get(e.expressionID).duplicate();
					IDtoExpression.put(i,e); // postpone replacing elements to after iteration to prevent errors
					//this.expressionOred.set(i, e);
					e.initiateExpression(depth+1);					
				} else {
					// remove this option, since we do not go deeper than max
					indicesToRemove.add(i);	
				}
				break;
			case Expression.typeAnded:
				if (e.containExpression()) {
					if (depth<depthMax) {
						numSelections++;
						e.initiateExpression(depth);
					} else {
						indicesToRemove.add(i);
					}				
				} else {
					numSelections++;
				}
				break;					
			}
			e.parent = this;
		}
		for(Integer id:IDtoExpression.keySet()) {
			this.expressionOred.set(id, IDtoExpression.get(id));
		}
		for(int i=0; i<indicesToRemove.size(); i++) {
			this.expressionOred.remove(indicesToRemove.get(i)-i);
		}
		
		if (numSelections>0) {
			selections.add(this);
			this.selection = 0;
			this.selectionMax = numSelections-1;
		}
	}
	
	/*
	 * generate the next distinct code to explore
	 * if no more distinct code exists, return null 
	 * before calling this function, do initiateExpressionTree()  
	 */
	static String generateNextDistinctCode() {	
		if (distinctCodeCount == 0) { // return 1st possible code
			distinctCodeCount++;
			return e0.generate();
		}
				
		for(int i=0;i<selections.size();i++) {
			Expression e = selections.get(i);
			if (e.isSelected() == false) {	// skip if not currently selected
				e.selection = 0;
				continue;
			}
			e.selection += 1;
			if (e.selection>e.selectionMax) {
				e.selection = 0;
				if (i == selections.size()-1) return null; // no more code to explore
				continue;
			}
			break;
		}
		
		distinctCodeCount++;
		return e0.generate(); // generate next distinct code
	}
	String generate() {
		switch(this.type()) {
		case typeString:
			return this.expressionString;
		case typeAnded:
			StringBuilder sb = new StringBuilder();
			for(int i=0;i<this.expressionAnded.size();i++) {
				Expression e = this.expressionAnded.get(i);
				sb.append(e.generate());
				if (i<this.expressionAnded.size()-1) sb.append(" ");
			}
			return sb.toString();
		case typeOred:
			return this.currentSelection().generate();
		default:
			return null;	
		}
	}
	Expression currentSelection() {		
		return this.expressionOred.get(this.selection);
	}
	boolean isSelected() {
		/*
		 * return true if this expression is part of currently selected code
		 *   e.g., parent expression is not selected among ORed options,
		 *   		it children also are not part of currently selected code
		 * if we can reach up to the root (e0) according to current selection,
		 *   then this expression is part of currently selected code
		 */
		Expression parent = this.parent;
		Expression child = this;
		while(parent != null) {
			if (parent.type() == typeOred 
					&& parent.currentSelection() != child) 
				return false;
			child = parent;
			parent = parent.parent;			
		}
		
		return true;
	}
	
	public static long startTime=0, endTime=0;
    public static void MeasureTime(String msg) 
    {
        if (startTime == 0) 
        {
            startTime = System.nanoTime();
            return;
        }
        
        endTime = System.nanoTime();
        System.out.println(msg + (endTime-startTime)/1000000 + "ms" + System.lineSeparator());
        
        startTime = endTime;
    }
    
	public static void main(String[] args) throws IOException, InterruptedException {		
		
		String path4codeSketch = null;
		String path4ioSample = null;
		for(int i=0;i<args.length;i=i+2) {
			if (args[i].equals("-c")) {path4codeSketch = args[i+1];}
			else if (args[i].equals("-i")) {path4ioSample = args[i+1];}
		}
		if (path4codeSketch == null || path4ioSample == null) {
			if (path4codeSketch == null) System.err.println("path for codesketch file is missing.");
			if (path4ioSample == null) System.err.println("path for i/o sample file is missing.");
			System.err.println("usage: -c [path for codesketch file] -i [path for i/o sample file]");
			System.exit(0);
		}
		
		Expression.parseFile(path4codeSketch);		
		System.out.println(Expression.toStringAllExpressions());		
		
		IOSamples io = IOSamples.parseFile(path4ioSample);
		
		Expression.initiateExpressionTree(10);
		
		MeasureTime("");
		String code;
		HashMap<String,Integer> codes = new HashMap<String,Integer>();
		while((code=Expression.generateNextDistinctCode()) != null) {
			System.out.println(code);
			if (codes.containsKey(code)) System.out.println("**** duplicate found ****");
			else codes.put(code, 1);
			
			IOSamples.Result result = io.validateCode2(code);
			System.out.println(result);
			if (result.numFailure == 0 && result.numSuccess > 0) System.out.println("**** code found ****");
		}
		System.out.println("A total of " + Expression.distinctCodeCount + " distinct codes found");
		
		System.out.println("*****");
		MeasureTime("");
	}

}
