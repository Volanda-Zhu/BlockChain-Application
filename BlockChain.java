/**
 *  Author: Xiaoyu Zhu
 *  Andrew id: xzhu4
 *  The project is to create a BlockChain.
 */


import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class BlockChain {
    String chainHash = null; // the most recent block
    List<Block> blockChainArray; // arraylist of block

    //constructor
    BlockChain(){
        blockChainArray = new ArrayList<>();
    }

    /**
     * Add a new Block to the BlockChain.
     * @param newBlock
     */
    public void addBlock(Block newBlock){
        Block lastBlock = getLatestBlock();
        newBlock.setPreviousHash(lastBlock.calculateHash());
        blockChainArray.add(newBlock);
        chainHash = newBlock.proofOfWork();
    }



    /**
     *
     * @return the size of the chain in blocks.
     */
    public int getChainSize() {
        return blockChainArray.size();
    }



    /**
     *
     * @return a reference to the most recently added Block.
     */
    public Block getLatestBlock(){
        return blockChainArray.get(blockChainArray.size() - 1);
    }

    /**
     *
     * @return the current system time
     */
    public Timestamp getTime(){
        return new Timestamp(System.currentTimeMillis());
    }



    /**
     *
     * @return hashes per second of the computer holding this chain.
     * It uses a simple string - "00000000" to hash.
     */
    public int hashesPerSecond(){
        Timestamp start = getTime();
        String s = "00000000";
        int cnt = 0;
        while(getTime().getTime() - start.getTime() <= 1000){
            cnt++;
            try{
                // Create a SHA256 digest
                MessageDigest digest;
                digest = MessageDigest.getInstance("SHA-256");
                // allocate room for the result of the hash
                byte[] hashBytes;
                // perform the hash
                digest.update(s.getBytes("UTF-8"), 0, s.length());

                // collect result
                hashBytes = digest.digest();
                StringBuffer hexString = new StringBuffer();
                for (int i = 0; i < hashBytes.length; i++) {
                    int halfbyte = (hashBytes[i] >>> 4) & 0x0F;
                    int two_halfs = 0;
                    do {
                        if ((halfbyte >= 0) && (halfbyte <= 9))
                            hexString.append((char) ('0' + halfbyte));
                        else
                            hexString.append((char) ('a' + (halfbyte - 10)));
                        halfbyte = hashBytes[i] & 0x0F;
                    } while (two_halfs++ < 1);
                }
            } catch (NoSuchAlgorithmException nsa) {
                System.out.println("No such algorithm exception thrown " + nsa);
            } catch (UnsupportedEncodingException uee) {
                System.out.println("Unsupported encoding exception thrown " + uee);
            }
        }
        return cnt;
    }



    /**
     * If the chain only contains one block, the genesis block at position 0,
     * this routine computes the hash of the block and checks that the hash has
     * the requisite number of leftmost 0's (proof of work) as specified in the difficulty field.
     *
     * If the chain has more than one block, check all the blocks.
     * If any of the hash in the previous block does not equal to the hash pointer of current block
     * or the proof of work is incorrect, return false;
     * otherwise, return true.
     * @return true if and only if the chain is valid
     */
    public boolean isChainValid(){

        int size = getChainSize();
        if(size == 1){
            String hashString = getLatestBlock().calculateHash();
            String difficultString = new String(new char[getLatestBlock().getDifficulty()]).replace('\0','0');
            if(hashString.substring(0,getLatestBlock().getDifficulty()).equalsIgnoreCase(difficultString) && hashString.equalsIgnoreCase(chainHash)){
                return true;
            }else{
                return false;
            }
        }else{
            for(int i = 1; i < size; i++){
                Block prev = blockChainArray.get(i - 1);
                Block curr = blockChainArray.get(i);
                String prevHash = prev.calculateHash();
                String currHash = curr.calculateHash();
                String prevDiff = new String(new char[prev.getDifficulty()]).replace('\0','0');
                String currDiff = new String(new char[curr.getDifficulty()]).replace('\0','0');
                if((!curr.getPreviousHash().equalsIgnoreCase(prevHash)) ||
                        (!prevHash.substring(0,prev.getDifficulty()).equalsIgnoreCase(prevDiff)) ||
                        (!currHash.substring(0,curr.getDifficulty()).equalsIgnoreCase(currDiff)) ){
                    return false;
                }
            }
            return true;
        }


    }


    /**
     * This routine repairs the chain.
     * It checks the hashes of each block and ensures that any illegal hashes are recomputed.
     * After this routine is run, the chain will be valid.
     * The routine does not modify any difficulty values.
     * It computes new proof of work based on the difficulty specified in the Block.
     *
     */
    public void repairChain(){
        for (int i = 0; i < getChainSize(); i++){
            Block block = blockChainArray.get(i);
            String hashString = block.calculateHash();
            String difficultString = new String(new char[block.getDifficulty()]).replace('\0','0');
            if(!hashString.substring(0, block.getDifficulty()).equalsIgnoreCase(difficultString)){
                String newHash = block.proofOfWork();
                if(i < getChainSize() - 1){
                    blockChainArray.get(i + 1).setPreviousHash(newHash);
                }else{
                    chainHash = newHash;
                }
            }
        }

    }

    /**
     *
     * @return a String representation of the entire chain is returned.
     */
    @Override
    public String toString(){
        String out = "{\"ds_chain\":[";
        for(Block block : blockChainArray){
            out += block.toString() + ",\n";
        }
        out +="], \"chainHash\": \"" + chainHash + "\"}";
        return out;
    }

}
