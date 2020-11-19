#define NOMINMAX 1
#define STRICT 1
#define WIN32_LEAN_AND_MEAN 1

// JDK
#include <jni.h>

// Windows Sockets
#include <windows.h>
#include <winsock2.h>
#include <ws2tcpip.h>
#include <mswsock.h>


#pragma comment (lib, "ws2_32.lib")
#pragma comment (lib, "mswsock.lib")


extern "C"
JNIEXPORT
jint JNICALL Java_br_dev_pedrolamarao_sandbox_Program_send (JNIEnv * env, jobject self, jstring host, jstring service, jstring path)
{
    auto host_c = env->GetStringUTFChars(host, nullptr);
    auto service_c = env->GetStringUTFChars(service, nullptr);
    auto path_c = env->GetStringUTFChars(path, nullptr);
        
    ADDRINFOA hint { 0, AF_INET, SOCK_STREAM, 0, 0, nullptr, nullptr, nullptr };
    ADDRINFOA * address {};
    int r0 = getaddrinfo(host_c, service_c, & hint, & address);
    if (r0 != 0) {
        // #TODO: report
        return -1;
    }
    
    auto socket = ::socket(address->ai_family, address->ai_socktype, address->ai_protocol);
    if (socket == INVALID_SOCKET) {
        int error = GetLastError();
        freeaddrinfo(address);
        return error;
    }
    
    int r1 = connect(socket, address->ai_addr, address->ai_addrlen);
    if (r1 == SOCKET_ERROR) {
        int error = GetLastError();
        freeaddrinfo(address);
        closesocket(socket);
        return error;
    }
    
    freeaddrinfo(address);
    
    auto file = CreateFileA(path_c, GENERIC_READ, FILE_SHARE_READ, nullptr, OPEN_EXISTING, 0, nullptr);
    if (file == INVALID_HANDLE_VALUE) {
        int error = GetLastError();
        closesocket(socket);
        return error;
    }
    
    int r2 = TransmitFile(socket, file, 0, 0, nullptr, nullptr, TF_DISCONNECT);
    if (r2 == FALSE) {
        int error = GetLastError();
        closesocket(socket);
        CloseHandle(file);
        return error;
    }
    
    closesocket(socket);
    CloseHandle(file);
    
    return 0;
}
