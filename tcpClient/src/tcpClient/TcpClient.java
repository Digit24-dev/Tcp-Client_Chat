package tcpClient;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class TcpClient {

	// 소켓 객체 선언
	private Socket sock = null;

	// 콘솔에서 호스트명, 포트번호 입력 받기 위한 스트림 객체
	private InputStreamReader isr1 = new InputStreamReader(System.in);
	private BufferedReader br = new BufferedReader(isr1);

	// 서버로 부터 읽어오기 위한 입력 스트림 객체
	private InputStream is;
	private InputStreamReader isr2;
	private BufferedReader readFromServer = null;

	// 서버로 전달할 출력 스트림 객체
	private OutputStream os;
	private OutputStreamWriter osw;
	static PrintWriter pw = null;

	// 시작 종료 플래그
	boolean endflag = false;

	// 생성자 - 연결 또는 연결 종료 
	public TcpClient() {
		if (endflag == false) {
			connect();
		} else if (endflag == true) {
			close();
		}
	}

	// 네트워크 연결을 위한 메소드
	public void connect() {
		try {
			// host, port 입력
			System.out.println("Host Name : ");
			String host = br.readLine();
			System.out.println("Port Number : ");
			int port = Integer.parseInt(br.readLine());

			// 소켓 객체 생성(socket open)
			sock = new Socket(host, port);
			System.out.println("Connection is successful. Socket opened....");

			// 입출력 스트림 객체 생성
			os = sock.getOutputStream();
			osw = new OutputStreamWriter(os, "UTF-8");
			pw = new PrintWriter(osw);

			is = sock.getInputStream();
			isr2 = new InputStreamReader(is, "UTF-8");
			readFromServer = new BufferedReader(isr2);

			// 콘솔에서 클라이언트의  id 읽어옴
			System.out.println("Input id : ");
			String id = br.readLine();

			// 입력된 클라이언트 id를 서버로 전달
			pw.println(id);
			pw.flush();
		} catch (Exception e) {
			if (!endflag) {
				e.printStackTrace();
			}
		}
	}

	// 네트워크 연결을 종료하기 위한 메소드
	public void close() {
		endflag = true;
		try {
			if (pw != null)
				pw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			if (readFromServer != null)
				readFromServer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			if (sock != null)
				sock.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 명령어를 서버로 전송하기 위한 메소드
	public void sendToServer(String str) {
		pw.println(str);
		pw.flush();
	}

	public static void main(String[] agrs) {
		TcpClient tc = new TcpClient();

		// 자식스레드 시작 - 서버가 전달하는 문자열을 읽어와 화면(표준 출력)에 출력
		ReadThread thread = new ReadThread(tc.sock, tc.is);
		thread.start();

		// 메인스레드-전달할 메시지를 키보드(표준입력)로 입력받아 서버로 전송
		String line = null;
		try {
			while ((line = tc.br.readLine()) != null) {
				tc.sendToServer(line);
				if (line.equals("/quit")) {
					tc.endflag = true;
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Client connection is closed.");
	}
}

// 자식 스레드 클래스 정의
// 서버가 전달하는 문자열을 읽어와 화면(표준 출력)에 출력시키는 스레드
class ReadThread extends Thread {
	private Socket sock = null;
	private InputStream readFromServer = null;
	
	private InputStreamReader isr = null;
	private BufferedReader br = null;
	private DataInputStream dis = null;
	
	// 생성자 - 소켓객체와 입력스트림 객체를 가져와 저장함
	public ReadThread(Socket sock, InputStream readFromServer) {
		this.sock = sock;
		this.readFromServer = readFromServer;
	}

	public void run() {
		try {
			String line = null;
			String[] parse;
			int length;
			byte[] buffer = new byte[1024];
			
			InputStreamReader isr = new InputStreamReader(readFromServer, "UTF-8");
			BufferedReader br = new BufferedReader(isr);
			DataInputStream dis = new DataInputStream(readFromServer);
			
			//서버로 부터 전송된 메시지 읽어서 출력
			while ((line = br.readLine()) != null) {
				
				if(line.contains("/fileTransfer..")) {
					parse = line.split(" ");
					String fileName = parse[1];
					FileOutputStream fos = new FileOutputStream(fileName);
					
					long fileSize = dis.readLong();
					long data = 0;
					
					try {
						while((length = dis.read(buffer)) != -1) {
							fos.write(buffer, 0, length);
							data += length;
							if(data == fileSize) break;
						}
						
						try {
							fos.close();
						}catch(Exception e) {
							e.printStackTrace();
						}
						System.out.println("작업 완료.");
					}catch(Exception e){
						System.out.println("전송 작업오류");
						e.printStackTrace();
					}
				}else {
					System.out.println(line);
				}
			}
		} catch (Exception e) {
			System.out.println("Socket closed.");
			e.printStackTrace();
		} finally {		
			try {
				if (readFromServer != null)
					readFromServer.close();
				if (br != null)
					br.close();
				if (dis != null)
					dis.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			try {
				if (sock != null)
					sock.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}
}