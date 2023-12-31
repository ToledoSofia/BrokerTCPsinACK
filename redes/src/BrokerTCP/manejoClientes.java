package BrokerTCP;

//import com.sun.
// //corba.se.spi.activation.Server;

import criptografia.Asimetrica;
import criptografia.Hash;
import criptografia.Mensaje;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;

class manejoClientes implements Runnable {
    private ServerSocket servidor;
    private ServidorBroker serv;
    private Socket so;
    private String mensajeRecibido;
    private PublicKey publicaCliente;
    private PrivateKey privadaServidor;

    public manejoClientes(ServerSocket servidor, Socket so, ServidorBroker sb,PublicKey publicaCliente, PrivateKey privada) {
        this.servidor = servidor;
        this.so = so;
        serv = sb;
        this.publicaCliente = publicaCliente;
        privadaServidor = privada;
    }

    @Override
    public void run() {
/*
        String mensaje2;
*/
        while (true) {
            mensajeRecibido = "";
            try {
                ObjectInputStream entrada = new ObjectInputStream(so.getInputStream());
                ObjectOutputStream salida;

                Mensaje mensaje1 = (Mensaje) entrada.readObject();
                String hasheado = new String(Asimetrica.desenciptarFirma(mensaje1.getFirma(),publicaCliente,"RSA"),"UTF8");
                mensajeRecibido = new String(Asimetrica.desencriptar(mensaje1.getEncriptadoPublica(),privadaServidor,"RSA" ),"UTF8");
                //tiene que enviar encriptandolo con lo del destino y acomodar cuando se agregan canales a los topicosClientes
                if(hasheado.equals(Hash.hashear(mensajeRecibido))){
                    if (mensajeRecibido.startsWith("s:") || mensajeRecibido.startsWith("S:")) {// suscripcion
                        System.out.println("+ suscripcion " + mensajeRecibido.substring(2));
                        String mensaje = mensajeRecibido.substring(2);

                        if(!serv.getTopicosClientes().keySet().contains(mensaje)){//si no existe el canal lo crea y guarda el cliente en la lista junto con la clave publica
                            HashMap<Socket, PublicKey> c = new HashMap<>();
                            c.put(so,publicaCliente);
                            HashMap<String,HashMap<Socket,PublicKey>>aux = serv.getTopicosClientes();
                            aux.put(mensaje,c);
                            serv.setTopicosClientes(aux);
                        }else{
                            HashMap<Socket, PublicKey> c = serv.getTopicosClientes().get(mensaje);
                            c.put(so,publicaCliente);
                            HashMap<String,HashMap<Socket,PublicKey>>aux = serv.getTopicosClientes();
                            aux.put(mensaje,c);
                            serv.setTopicosClientes(aux);
                        }
                    /*serv.getTopicosClientes()
                        .computeIfAbsent(mensaje, k -> new HashMap<>())
                        .add(so);*/
                    } else if (mensajeRecibido.startsWith("m:") || mensajeRecibido.startsWith("M:")) {//mensaje
                        String[] partes = mensajeRecibido.split(":");
                        String canal = partes[1];
                        String mensajeSolo = partes[2];
                        mensajeSolo ="CANAL: " + canal + ":" + mensajeSolo;

                        HashMap<Socket, PublicKey> socketsCanal = serv.getTopicosClientes().get(canal);
                        if (socketsCanal != null) {
                            for (Socket s : socketsCanal.keySet()) {//manda el mnsj a todos los suscriptos al canal
                                Mensaje mensajeEncriptado = new Mensaje(
                                        Asimetrica.firmar(Hash.hashear(mensajeSolo).getBytes("UTF8"),privadaServidor,"RSA"),
                                        Asimetrica.encriptar(mensajeSolo.getBytes("UTF8"),socketsCanal.get(s),"RSA")
                                );
                                salida = new ObjectOutputStream(s.getOutputStream());
                                salida.writeObject(mensajeEncriptado);
                                salida.flush();
                                //salida.writeUTF("\nCANAL: " + canal + " = " + mensajeSolo);
                            }
                        }
                    }
                }else{
                    System.out.println("no coincide el hash");
                }
                System.out.println(mensajeRecibido);
            } catch (IOException e) {
                //throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    }
}


