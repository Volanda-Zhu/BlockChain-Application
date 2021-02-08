/**
 *  Author: Xiaoyu Zhu
 *  Andrew id: xzhu4
 *  The project is a TCP client for the BlockChain.
 */

import org.json.simple.JSONObject;
import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

public class BlockChainClientTCP {
    public static void main(String args[]){

        startClient(); //start the proxy
    }

    /**
     * This is a proxy which encapsulates the communication code
     * The client will send a request to the server
     * The information includes id, public key,option, signature and other needed info.
     */
    // 1. On average, it takes longer time for a block of difficulty 5 than 4.

    // 2. Apart from the test provided by the instructor, I tried adding blocks of difficulty 4, the average time is around 123 millionseconds.
    // On the other hand, it takes 6517 million seconds to add blocks of difficulty 5 for 15 times.

    // 3. The times the blockchain takes to run the isChainValid() method for these two
    // levels of difficulty are almost the same; they are around 0 milliseconds.

    // 4. After corrupting one block, the time it takes to run the chainRepair() method for the chain
    // with difficulty 4 is around 842 milliseconds. However, it takes around 13234 millionseconds to run the
    // chainRepair() for the chain with difficulty 5.

    // 5. Therefore, the block chain is more efficient and faster when dealing with a block of lower difficulty for method addBlock(),chainRepair().
    // It takes equal time to verify, i.e. isChainValid() method.
    // These results are consistent with the assumption.
    public static void startClient() {
        System.out.println("Client running.");
        Socket clientSocket =  null;
        try{
            // If we get here, then we are now connected to a server.
            int serverPort = 7777; //port
            clientSocket = new Socket("localhost", serverPort);//start a new socket
            BufferedReader typed = new BufferedReader(new InputStreamReader(System.in));

            // Set up "out" to read from the socket
            OutputStream outputStream = clientSocket.getOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(outputStream);

            // Set up "in" to read from the socket
            InputStream inputStream = clientSocket.getInputStream();
            ObjectInputStream in = new ObjectInputStream(inputStream);

            BigInteger[] keys = getKeys();
            String publicKey = keys[0].toString() + keys[1].toString(); //public key, keys[0] is e, keys[1] is n
            String id = getId(publicKey); //calculate the user id
            System.out.println("The client id is " + id);

            while(true){
                String information = id + "," + publicKey;
                // menu bar
                printMenu();

                int option = Integer.parseInt(typed.readLine());
                information += "," + option;

                //create a new jsonObject to store the information
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", id);
                jsonObject.put("e", keys[0]);
                jsonObject.put("n", keys[1]);
                jsonObject.put("option", option);

                //add a new block to the chain
                if(option == 1){
                    System.out.println("Enter difficulty > 0");
                    int difficulty = Integer.valueOf(typed.readLine());  //enter the difficulty
                    jsonObject.put("difficulty",difficulty);

                    System.out.println("Enter transaction");
                    String transaction = typed.readLine();
                    jsonObject.put("transaction",transaction);

                    information+= "," + difficulty + "," + transaction;
                }
                // corrupt the entered block using new entered string
                else if(option == 4){
                    System.out.println("Corrupt the Blockchain");
                    System.out.println("Enter block ID of block to Corrupt");
                    int blockID = Integer.valueOf(typed.readLine()) ;

                    jsonObject.put("blockToCorrupt",blockID);
                    System.out.println("Enter new data for block " + blockID);

                    String newData = typed.readLine();
                    jsonObject.put("newData",newData);

                    information += "," + blockID + "," + newData;
                }

                String signature = sign(information, keys[2],keys[1]);
                jsonObject.put("signature",signature);

                //send the request to the server
                out.writeObject(jsonObject);
                outputStream.flush();

                if(option == 6){
                    break;
                }

                JSONObject jsonObjectReceive = (JSONObject)in.readObject();
                if(jsonObjectReceive.get("error")==null) {
                    System.out.println((String)jsonObjectReceive.get("res"));
                }else{
                    System.out.println("BlockChainClientTCP.startClient");
                }

            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }finally{
            try{
                if(clientSocket != null){
                    clientSocket.close();
                }
            }catch (IOException e) {

            }
        }
    }

    /**
     * Print the menu bar to the client
     */
    public static void printMenu() {
        System.out.println("0. View basic blockchain status.");
        System.out.println("1. Add a transaction to the blockchain.");
        System.out.println("2. Verify the blockchain.");
        System.out.println("3. View the blockchain.");
        System.out.println("4. Corrupt the chain");
        System.out.println("5. Hide the corruption by repairing the chain.");
        System.out.println("6. Exit.");
    }

    /**
     * This method sign the request.
     * By using its private key (d and n), the client will encrypt the hash of the message it sends to the server.
     * The signature will be added to each request.
     * @param information
     * @param d
     * @param n
     * @return signature
     */
    private static String sign(String information, BigInteger d, BigInteger n) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] bytesOfMessageToCheck = information.getBytes("UTF-8");
        // compute the digest of the message with SHA-256
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(bytesOfMessageToCheck);
        // we add a 0 byte as the most significant byte to keep
        // the value to be signed non-negative.
        byte[] positiveHash = new byte[hash.length + 1];
        positiveHash[0] = 0;   // most significant set to 0
        for (int i = 0; i < hash.length; i++) {
            positiveHash[i+1] = hash[i]; // take a byte from SHA-256
        }

        BigInteger m = new BigInteger(positiveHash);

        // encrypt the digest with the private key
        BigInteger c = m.modPow(d, n);
        return c.toString();
    }

    /**
     * Hash a string using SHA-256
     * take the least significant 20 bytes of the hash
     * @param publicKey
     * @return id
     */
    public static String getId(String publicKey) {
        String newpublicKey = publicKey.toLowerCase();
        try {
            // Create a SHA256 digest
             byte[] hashBytes = publicKey.getBytes("UTF-8");
             MessageDigest md = MessageDigest.getInstance("SHA-256");
             byte[] newHashBytes = md.digest(hashBytes);
             byte[] newHashByte = Arrays.copyOfRange(hashBytes, hashBytes.length - 20, hashBytes.length);
             return new BigInteger(newHashByte).toString();
        }
        catch (NoSuchAlgorithmException nsa) {
            System.out.println("No such algorithm exception thrown " + nsa);
        }
        catch (UnsupportedEncodingException uee ) {
            System.out.println("Unsupported encoding exception thrown " + uee);
        }
        return "";
    }

    /**
     * Create an array to store the keys
     * e is the exponent of the public key
     * n is the modulus for both the private and public keys
     * d is the exponent of the private key
     * @return [e,n,d]
     */
    public static BigInteger[] getKeys() {

        BigInteger n; // n is the modulus for both the private and public keys
        BigInteger e; // e is the exponent of the public key
        BigInteger d; // d is the exponent of the private key
        BigInteger[] keys = new BigInteger[3];

        // Step 1: Generate two large random primes.
        Random rnd = new Random();
        BigInteger p = new BigInteger(400, 100, rnd);
        BigInteger q = new BigInteger(400, 100, rnd);

        // Step 2: Compute n by the equation n = p * q.
        n = p.multiply(q);
        // Step 3: Compute phi(n) = (p - 1) * ( q - 1)
        BigInteger phi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));
        //Step 4: Select a small odd integer e that is relatively prime to phi(n)
        e = new BigInteger("65537");
        // Step 5: Compute d as the multiplicative inverse of e modulo phi(n)
        d = e.modInverse(phi);

        //Step 6: store value
        keys[0] = e;
        keys[1] = n;
        keys[2] = d;
        return keys;

    }
}
