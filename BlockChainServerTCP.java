/**
 *  Author: Xiaoyu Zhu
 *  Andrew id: xzhu4
 *  The project is a TCP server for the BlockChain.
 */

import org.json.simple.JSONObject;
import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Arrays;



public class BlockChainServerTCP {
    BlockChain bc = new BlockChain();


    /**
     * This is a proxy which encapsulates the communication code
     * The server will send a request to the client
     * The information includes id, public key,option, signature and other needed info.
     */
    public static void main(String args[]){
        BlockChainServerTCP server = new BlockChainServerTCP();
        Block firstBlock = new Block(0,server.bc.getTime(),"Genesis",2);
        firstBlock.setPreviousHash("");
        server.bc.chainHash = firstBlock.proofOfWork();
        server.bc.blockChainArray.add(firstBlock);
        server.startServer();
    }

    /**
     * This is a proxy which encapsulates the communication code
     * The server will first verify the identity of the client
     * If the two verification pass, it will send the information back to the client
     *
     */
    public  void startServer() {
        System.out.println("Server running.");
        Socket serverSocket = null;

        try {
            int serverPort = 7777; // the server port we are using
            // Create a new server socket
            ServerSocket listenSocket = new ServerSocket(serverPort);

            /*
             * Block waiting for a new connection request from a client.
             * When the request is received, "accept" it, and the rest
             * the tcp protocol handshake will then take place, making
             * the socket ready for reading and writing.
             */
            while(true){

                serverSocket = listenSocket.accept();
                // If we get here, then we are now connected to a client.
                // Set up "in" to read from the client socket
                InputStream inputStream = serverSocket.getInputStream();
                ObjectInputStream in = new ObjectInputStream(inputStream);

                // Set up "out" to read from the socket
                OutputStream outputStream = serverSocket.getOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(outputStream);

                while (true) {
                    //receive the jsonobject from the client
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject = (JSONObject) in.readObject();

                    } catch (IOException e) {
                        break;
                    } catch (ClassNotFoundException e) {
                        break;
                    }
                    String userID = (String) jsonObject.get("id"); //userId
                    BigInteger e = (BigInteger) (jsonObject.get("e")); // e is the exponent of the public key
                    BigInteger n = (BigInteger) jsonObject.get("n"); // n is the modulus for both the private and public keys
                    int option = (int) jsonObject.get("option"); //option
                    String publicKey = e.toString() + n.toString(); //public key is concatenation of e and n
                    String signature = (String) jsonObject.get("signature"); //signature
                    String messageToCheck = userID + "," + publicKey + "," + option; //the message need to be checked

                    JSONObject responseObject = new JSONObject();
                    // Verify if the public key is matched
                    if (!verifyPublicKey(publicKey, userID)) {
                        System.out.println("Verification error: the public key does not match!");
                        responseObject.put("error", "error 1: the public key does not match!");
                        break;
                    }

                    //option is 1, add difficulty and transaction to the checking message
                    if (option == 1) {
                        messageToCheck += "," + jsonObject.get("difficulty") + "," + jsonObject.get("transaction");
                    }
                    //option is 4, add blockToCorrupt and newData to the checking message
                    else if (option == 4) {
                        messageToCheck += "," + jsonObject.get("blockToCorrupt") + "," + jsonObject.get("newData");
                    }

                    // Verify if the signature is matched
                    if (!verifySignature(messageToCheck, signature, e, n)) {
                        System.out.println("Verification error: the signature does not match!");
                        responseObject.put("error", "error 2: the signature does not match!");
                        break;
                    }

                    String res = new String();

                    switch (option) {
                        //case 0: display the current chain
                        case 0: {
                            res += "Current size of chain: " + bc.getChainSize() + "\n";
                            res += "Current hashes per second by this machine: " + bc.hashesPerSecond() + "\n";
                            res += "Difficulty of most recent block: " + bc.getLatestBlock().getDifficulty() + "\n";
                            res += "Nonce for most recent block: " + bc.getLatestBlock().getNonce() + "\n";
                            res += "Chain hash: " + bc.chainHash;
                            break;
                        }
                        // case 1: add anew block to the end
                        case 1: {
                            int difficulty = Integer.parseInt(jsonObject.get("difficulty").toString());
                            String transaction = jsonObject.get("transaction").toString();
                            Timestamp start = bc.getTime();
                            Block nextblock = new Block(bc.getChainSize(), start, transaction, difficulty);
                            bc.addBlock(nextblock);
                            Timestamp end = bc.getTime();
                            res += "Total execution time to add this block was " + (end.getTime() - start.getTime()) + " millionseconds" + "\n";
                            break;
                        }
                        // case 2: verify the whole chain.
                        case 2: {
                            res += "Verifying the entire chain  \n";
                            Timestamp start = bc.getTime();
                            res += "Chain verification: " + bc.isChainValid() + "\n";
                            Timestamp end = bc.getTime();
                            res += "Total execution time required to verify the chain was " + (end.getTime() - start.getTime()) + " millionseconds" + "\n";
                            break;
                        }
                        // case 3: display the whole chain (JSON)
                        case 3: {
                            res += "View the BlockChain \n";
                            res += bc.toString() + "\n";
                            break;
                        }
                        // case 4: corrupt the chain with new Data
                        case 4: {
                            res += "Corrupt the Blockchain \n";
                            res += "Enter block ID of block to Corrupt \n";
                            int blockID = (int) jsonObject.get("blockToCorrupt");
                            res += "Enter new data for block " + blockID + "\n";
                            String newData = (String) jsonObject.get("newData");
                            bc.blockChainArray.get(blockID).setData(newData);
                            res += "Block " + blockID + " now holds " + newData + "\n";
                            break;
                        }
                        // case 5: recompute the proof of work, repair the chain
                        case 5: {
                            res += "Reparing the entire chain\n";
                            Timestamp start = bc.getTime();
                            bc.repairChain();
                            Timestamp end = bc.getTime();
                            res +="Total execution required to repair the chain was " + (end.getTime() - start.getTime()) + " millionseconds";
                            break;
                        }
                        // case 6: exit
                        case 6: {
                            continue;
                        }
                        default:
                            throw new IllegalStateException();

                    }

                    responseObject.put("res", res);
                    // send back to the client
                    out.writeObject(responseObject);
                    outputStream.flush();
                }
            }

        } catch (IOException  e){
            System.out.println("IO Exception "+ e.getMessage());
        } catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
        }finally{
            try{
                if(serverSocket != null){
                    serverSocket.close();
                }
            } catch (IOException e) {

            }
        }
    }


    /**
     * verify the signature
     * @param messageToCheck
     * @param signature
     * @param e
     * @param n
     * @return true if the signature is matched
     */
    public static boolean verifySignature(String messageToCheck, String signature, BigInteger e, BigInteger n) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        // Take the encrypted string and make it a big integer
        BigInteger encryptedHash = new BigInteger(signature);
        // Decrypt it
        BigInteger decryptedHash = encryptedHash.modPow(e, n);
        // Get the bytes from messageToCheck
        byte[] bytesOfMessageToCheck = messageToCheck.getBytes("UTF-8");
        // compute the digest of the message with SHA-256
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] messageToCheckDigest = md.digest(bytesOfMessageToCheck );
        byte[] positiveHash = new byte[messageToCheckDigest.length + 1];
        for (int i = 0; i < messageToCheckDigest.length; i++) {
            positiveHash[i+1] = messageToCheckDigest[i]; // take a byte from SHA-256
        }
        BigInteger m = new BigInteger(positiveHash);
        // if it is properly signed, return true
        if(m.compareTo(decryptedHash) == 0) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * verify the publicKey
     * @param publicKey
     * @param userID
     * @return true if the public key is matched, otherwise, false
     */
    public static boolean verifyPublicKey(String publicKey, String userID) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        if(getId(publicKey).equals(userID)){
            return true;
        } else{
            return false;
        }
    }

    /**
     * Get the id by hashing the public key
     * @param publicKey
     * @return
     */
    public static String getId(String publicKey) {
        try {
            String newpublicKey = publicKey.toLowerCase();
            byte[] hashBytes = publicKey.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("SHA-256"); // Create a SHA256 digest
            byte[] newHashBytes = md.digest(hashBytes);
            newHashBytes = Arrays.copyOfRange(hashBytes, hashBytes.length - 20, hashBytes.length);

            return new BigInteger(newHashBytes).toString();
        }
        catch (NoSuchAlgorithmException nsa) {
            System.out.println("No such algorithm exception thrown " + nsa);
        }
        catch (UnsupportedEncodingException uee ) {
            System.out.println("Unsupported encoding exception thrown " + uee);
        }
        return "";
    }
}
