package br.dev.pedrolamarao.sandbox;

public class Program
{
    static
    {
        NativeLoader.loadLibrary(Program.class.getClassLoader(), "sendfile-jni");
    }
    
    public native int send (String host, String service, String file);
    
    public static void main (String[] args)
    {
    	final Program program = new Program();
    	final int status = program.run(args);
    	System.exit(status);
    }
    
    public int run (String[] args)
    {
    	if (args.length < 3) {
    		System.err.println("usage: Program [host] [service] [file]");
    		return -1;
    	}
    	
    	final String host = args[0];
    	final String service = args[1];
    	final String file = args[2];
    	
    	final int result = send(host, service, file);
    	if (result != 0) {
    		System.err.println("error: communication failure: " + String.format("%d", result));
    	}
    	
    	return result;
    }
}
