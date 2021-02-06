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

	// ���� ��ü ����
	private Socket sock = null;

	// �ֿܼ��� ȣ��Ʈ��, ��Ʈ��ȣ �Է� �ޱ� ���� ��Ʈ�� ��ü
	private InputStreamReader isr1 = new InputStreamReader(System.in);
	private BufferedReader br = new BufferedReader(isr1);

	// ������ ���� �о���� ���� �Է� ��Ʈ�� ��ü
	private InputStream is;
	private InputStreamReader isr2;
	private BufferedReader readFromServer = null;

	// ������ ������ ��� ��Ʈ�� ��ü
	private OutputStream os;
	private OutputStreamWriter osw;
	static PrintWriter pw = null;

	// ���� ���� �÷���
	boolean endflag = false;

	// ������ - ���� �Ǵ� ���� ���� 
	public TcpClient() {
		if (endflag == false) {
			connect();
		} else if (endflag == true) {
			close();
		}
	}

	// ��Ʈ��ũ ������ ���� �޼ҵ�
	public void connect() {
		try {
			// host, port �Է�
			System.out.println("Host Name : ");
			String host = br.readLine();
			System.out.println("Port Number : ");
			int port = Integer.parseInt(br.readLine());

			// ���� ��ü ����(socket open)
			sock = new Socket(host, port);
			System.out.println("Connection is successful. Socket opened....");

			// ����� ��Ʈ�� ��ü ����
			os = sock.getOutputStream();
			osw = new OutputStreamWriter(os, "UTF-8");
			pw = new PrintWriter(osw);

			is = sock.getInputStream();
			isr2 = new InputStreamReader(is, "UTF-8");
			readFromServer = new BufferedReader(isr2);

			// �ֿܼ��� Ŭ���̾�Ʈ��  id �о��
			System.out.println("Input id : ");
			String id = br.readLine();

			// �Էµ� Ŭ���̾�Ʈ id�� ������ ����
			pw.println(id);
			pw.flush();
		} catch (Exception e) {
			if (!endflag) {
				e.printStackTrace();
			}
		}
	}

	// ��Ʈ��ũ ������ �����ϱ� ���� �޼ҵ�
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

	// ��ɾ ������ �����ϱ� ���� �޼ҵ�
	public void sendToServer(String str) {
		pw.println(str);
		pw.flush();
	}

	public static void main(String[] agrs) {
		TcpClient tc = new TcpClient();

		// �ڽĽ����� ���� - ������ �����ϴ� ���ڿ��� �о�� ȭ��(ǥ�� ���)�� ���
		ReadThread thread = new ReadThread(tc.sock, tc.is);
		thread.start();

		// ���ν�����-������ �޽����� Ű����(ǥ���Է�)�� �Է¹޾� ������ ����
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

// �ڽ� ������ Ŭ���� ����
// ������ �����ϴ� ���ڿ��� �о�� ȭ��(ǥ�� ���)�� ��½�Ű�� ������
class ReadThread extends Thread {
	private Socket sock = null;
	private InputStream readFromServer = null;
	
	private InputStreamReader isr = null;
	private BufferedReader br = null;
	private DataInputStream dis = null;
	
	// ������ - ���ϰ�ü�� �Է½�Ʈ�� ��ü�� ������ ������
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
			
			//������ ���� ���۵� �޽��� �о ���
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
						System.out.println("�۾� �Ϸ�.");
					}catch(Exception e){
						System.out.println("���� �۾�����");
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