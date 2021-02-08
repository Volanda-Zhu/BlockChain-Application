/**
 *  Author: Xiaoyu Zhu
 *  Andrew id: xzhu4
 *  The project is a class of Block.
 */
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import com.google.gson.Gson;


public class Block {
    private int index; // the position of the block in the chain, The first block (the so called Genesis block) has an index of 0.
    private Timestamp timestamp; //time of the block's creation
    private String data; // the block's transaction details
    private String previousHash; //the SHA256 hash of a block's parent. This is also called a hash pointer.
    private BigInteger nonce = BigInteger.valueOf(0); //a BigInteger value specified by a small integer representing the number of leading hex the hash must have.
    private int difficulty; //an int that specifies the exact number of left most hex digits needed by a proper hash

    //This the Block constructor.
    public Block(int index, Timestamp timestamp, String data, int difficulty) {
        this.index = index;
        this.timestamp =  timestamp;
        this.data = data;
        this.difficulty = difficulty;
    }



    /**
     * This method computes a hash of the concatenation of the index, timestamp, data, previousHash, nonce, and difficulty.
     * First, concatenate the index, timestamp, data, previousHash, nonce and the difficulty
     * Next, calculate the hashcode of the concatenation
     * @return a String holding Hexadecimal characters
     */
    public String calculateHash(){
        StringBuilder sb = new StringBuilder();
        //String concat = index + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(timestamp) + data + previousHash + nonce + difficulty;
        //sb.append(concat);
        sb.append(index).append(timestamp).append(data).append(previousHash).append(nonce).append(difficulty);
        String stringToBeHashed = sb.toString().toUpperCase();
        //the difficulty is 3, the hash must have three leading hex 0's (or,1 and 1/2 bytes). Each hex digit represents 4 bits.
        try{
            // new SHA256 digest
            MessageDigest digest;
            digest = MessageDigest.getInstance("SHA-256");
            // allocate space for the hash
            digest.update(stringToBeHashed.getBytes("UTF-8"), 0, stringToBeHashed.length());
            // store the result in a hexString
            byte[] hashBytes = digest.digest();
            StringBuffer hexString = new StringBuffer(); // This will contain hash as hexdecimal
            int byteLength = hashBytes.length;
            for(int i = 0; i < byteLength; i++){
                int oneByte = 0;
                int halfByte = (hashBytes[i] >>> 4) & 0x0F;  // 1/2 byte
                while(oneByte <= 1){
                    if (halfByte >= 0  && halfByte <= 9){
                        hexString.append((char)('0' + halfByte));  //when byte is in [0, 9]
                    }
                    else{
                        hexString.append((char)('A' + (halfByte - 10))); // then byte is in ['A', 'F'] // A? or a?
                    }
                    halfByte = hashBytes[i] & 0x0F;
                    oneByte++;
                }
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }


    /**
     * This method returns the nonce for this block.
     * @return a BigInteger representing the nonce for this block.
     */
    public BigInteger getNonce() {
        return nonce;
    }


    /**
     * The proof of work methods finds a good hash. It increments the nonce until it produces a good hash.
     * @return  a String with a hash that has the appropriate number of leading hex zeroes.
     */
    public String proofOfWork(){

        String hashString = calculateHash();
        char[] difficultyTochar = new char[difficulty];
        String target = new String(difficultyTochar).replace('\0','0');
        // Validate if block is mined correctly
        while(!hashString.substring(0,difficulty).equalsIgnoreCase(target)){
            nonce = nonce.add(new BigInteger("1"));
            hashString = calculateHash();
        }
        return hashString;
    }


    /**
     * This method returns the difficulty which is the number of hex 0's a proper hash must have.
     * @return  an int representing the difficulty.
     */
    public int getDifficulty() {
        return difficulty;
    }

    /**
     * Set difficulty
     * Diffifulty determines how much work is required to produce a proper hash
     * @param difficulty
     */
    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    /**
     *
     * @return A JSON representation of all of this block's data is returned.
     */
    @Override
    public String toString(){
        Map jsonObject = new LinkedHashMap();
        jsonObject.put("index", index);
        jsonObject.put("time stamp",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp));
        jsonObject.put("tx ",data);
        jsonObject.put("PrevHash",previousHash);
        jsonObject.put("nonce",nonce);
        jsonObject.put("difficulty",difficulty);
        Gson gson = new Gson();
        return gson.toJson(jsonObject, LinkedHashMap.class);

    }

    /**
     * set previousHash - a hashpointer to this block's parent
     * @param previousHash
     */
    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    /**
     *
     * @return previousHash - a hashpointer to this block's parent
     */
    public String getPreviousHash() {
        return previousHash;
    }

    /**
     *
     * @return index of block
     */
    public int getIndex() {
        return index;
    }


    /**
     * set index of block
     * @param index
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * set timestamp of this block
     * @param timestamp
     */
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }


    /**
     *
     * @return get timestamp of this block
     */
    public Timestamp getTimestamp() {
        return timestamp;
    }

    /**
     * get data of this block
     * @return
     */
    public String getData() {
        return data;
    }

    /**
     * set timestamp of this block
     * @param data
     */
    public void setData(String data) {
        this.data = data;
    }


}