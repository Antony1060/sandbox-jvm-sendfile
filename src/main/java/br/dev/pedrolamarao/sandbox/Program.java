package br.dev.pedrolamarao.sandbox;

import static jdk.incubator.foreign.CLinker.C_INT;
import static jdk.incubator.foreign.CLinker.C_LONGLONG;
import static jdk.incubator.foreign.CLinker.C_POINTER;

import java.lang.invoke.MethodType;

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.LibraryLookup;
import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryLayout.PathElement;
import jdk.incubator.foreign.NativeScope;

public class Program
{    
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

    private static final int AF_INET = 2;
    
    private static final int FILE_SHARE_READ = 1;
    
    private static final int GENERIC_READ = 0x80000000;
    
    private static final int INVALID_SOCKET = -1;
    
    private static final int OPEN_EXISTING = 3; 
    
    private static final int SD_BOTH = 2;
    
    private static final int SOCK_STREAM = 1;
    
    public int send (String host, String service, String file)
    {
    	// memory layout
    	
    	final var addrinfo = MemoryLayout.ofStruct(
    		// int ai_flags
			C_INT.withName("flags"),
			// int ai_family
			C_INT.withName("family"),
			// int ai_socktype
			C_INT.withName("type"),
			// int ai_protocol
			C_INT.withName("protocol"),
			// size_t ai_addrlen
			C_LONGLONG.withName("addrlen"),
			// char * ai_canonname
			C_POINTER.withName("canonname"),
			// sockaddr * ai_addr
			C_POINTER.withName("addr"),
			// addrinfo * ai_next
			C_POINTER.withName("next")
		);
    	
    	// method handles
    	
    	final var linker = CLinker.getInstance();

    	final var kernel32 = LibraryLookup.ofLibrary("Kernel32");
    	
    	final var getlasterror = linker.downcallHandle(
			kernel32.lookup("GetLastError").get(),
			MethodType.methodType(int.class),
			FunctionDescriptor.of(C_INT)
		);
    	
    	final var createFile = linker.downcallHandle(
			kernel32.lookup("CreateFileA").get(),
			MethodType.methodType(MemoryAddress.class, MemoryAddress.class, int.class, int.class, MemoryAddress.class, int.class, int.class, MemoryAddress.class),
			FunctionDescriptor.of(C_POINTER,           C_POINTER,           C_INT,     C_INT,     C_POINTER,           C_INT,     C_INT,     C_POINTER)
		);

    	final var closeHandle = linker.downcallHandle(
			kernel32.lookup("CloseHandle").get(),
			MethodType.methodType(int.class, MemoryAddress.class),
			FunctionDescriptor.of(C_INT,     C_POINTER)
		);
    	
    	final var ws2_32 = LibraryLookup.ofLibrary("Ws2_32");

    	final var getaddrinfo = linker.downcallHandle(
    		ws2_32.lookup("getaddrinfo").get(),
			MethodType.methodType(int.class, MemoryAddress.class, MemoryAddress.class, MemoryAddress.class, MemoryAddress.class),
			FunctionDescriptor.of(C_INT,     C_POINTER,           C_POINTER,           C_POINTER,           C_POINTER)
		);

    	final var freeaddrinfo = linker.downcallHandle(
    		ws2_32.lookup("freeaddrinfo").get(),
			MethodType.methodType(int.class, MemoryAddress.class),
			FunctionDescriptor.of(C_INT,     C_POINTER          )
		);

    	final var socket = linker.downcallHandle(
    		ws2_32.lookup("socket").get(),
			MethodType.methodType(int.class, int.class, int.class, int.class),
			FunctionDescriptor.of(C_INT,     C_INT,     C_INT,     C_INT)
		);

    	final var connect = linker.downcallHandle(
			ws2_32.lookup("connect").get(),
			MethodType.methodType(int.class, int.class, MemoryAddress.class, int.class),
			FunctionDescriptor.of(C_INT,     C_INT,     C_POINTER,           C_INT)
		);

    	final var shutdown = linker.downcallHandle(
			ws2_32.lookup("shutdown").get(),
			MethodType.methodType(int.class, int.class, int.class),
			FunctionDescriptor.of(C_INT,     C_INT,     C_INT)
		);

    	final var closesocket = linker.downcallHandle(
			ws2_32.lookup("closesocket").get(),
			MethodType.methodType(int.class, int.class),
			FunctionDescriptor.of(C_INT,     C_INT)
		);
    	
    	final var mswsock = LibraryLookup.ofLibrary("Mswsock");

    	final var transmitFile = linker.downcallHandle(
			mswsock.lookup("TransmitFile").get(),
			MethodType.methodType(int.class, int.class, MemoryAddress.class, int.class, int.class, MemoryAddress.class, MemoryAddress.class, int.class),
			FunctionDescriptor.of(C_INT,     C_INT,     C_POINTER,           C_INT,     C_INT,     C_POINTER,           C_POINTER,           C_INT)
		);
    	
    	// downcalls
    	
    	try (var scope = NativeScope.unboundedScope())
		{
    		// resolve address
    		
			final var host_c = CLinker.toCString(host, scope);
			final var service_c = CLinker.toCString(service, scope);
			final var hint = scope.allocate(addrinfo).fill((byte) 0);
			addrinfo.varHandle(int.class, PathElement.groupElement("family")).set(hint, AF_INET);
			addrinfo.varHandle(int.class, PathElement.groupElement("type")).set(hint, SOCK_STREAM);
    		final var addressRef = scope.allocate(C_POINTER, MemoryAddress.NULL);
			final var r0 = (int) getaddrinfo.invokeExact(host_c.address(), service_c.address(), hint.address(), addressRef.address());
			if (r0 != 0) {
				final var error = (int) getlasterror.invokeExact();
				return error;
			}
			
			// UNSAFE! JVM cannot assert this is true!
			final var address = MemoryAccess.getAddress(addressRef).asSegmentRestricted(addrinfo.byteSize());

			final var addressFamily = (int) addrinfo.varHandle(int.class, PathElement.groupElement("family")).get(address);
			final var addressType = (int) addrinfo.varHandle(int.class, PathElement.groupElement("type")).get(address);
			final var addressProtocol = (int) addrinfo.varHandle(int.class, PathElement.groupElement("protocol")).get(address);
			final var addressData = MemoryAccess.getAddressAtOffset(address, 32); // #TODO: bug: unsupported carrier: MemoryAccess
			final var addressLength = (long) addrinfo.varHandle(long.class, PathElement.groupElement("addrlen")).get(address);
			
			// acquire socket
			
			final var socketHandle = (int) socket.invokeExact(addressFamily, addressType, addressProtocol);
			if (socketHandle == INVALID_SOCKET) {
				final var error = (int) getlasterror.invokeExact();
				freeaddrinfo.invoke(address);
				return error;
			}
			
			// connect socket
			
			final var r1 = (int) connect.invokeExact(socketHandle, addressData, (int) addressLength);
			if (r1 == -1) {
				final var error = (int) getlasterror.invokeExact();
				closesocket.invoke(socketHandle);
				freeaddrinfo.invoke(address);
				return error;
			}

			freeaddrinfo.invoke(address.address());
			
			// acquire file
			
			final var path_c = CLinker.toCString(file, scope);
			final var fileHandle = (MemoryAddress) createFile.invokeExact(path_c.address(), GENERIC_READ, FILE_SHARE_READ, MemoryAddress.NULL, OPEN_EXISTING, 0, MemoryAddress.NULL);
			if (fileHandle == MemoryAddress.NULL) {
				final var error = (int) getlasterror.invokeExact();
				closesocket.invoke(socketHandle);
				return error;
			}
			
			final var r2 = (int) transmitFile.invokeExact(socketHandle, fileHandle, 0, 0, MemoryAddress.NULL, MemoryAddress.NULL, 0);
			if (r2 != 0) {
				final var error = (int) getlasterror.invokeExact();
				closeHandle.invoke(fileHandle);
				closesocket.invoke(socketHandle);
				return error;
			}
			
			closeHandle.invoke(fileHandle);
			
			shutdown.invoke(socketHandle, SD_BOTH);
			
			closesocket.invoke(socketHandle);
		} 
    	catch (RuntimeException e)
    	{
    		throw e;
    	}
    	catch (Throwable e)
		{
    		throw new RuntimeException("send: oops", e);
		}

    	return 0;
    }
}
