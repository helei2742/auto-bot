//package cn.com.helei.DepinBot.core.util;
//
//import org.web3j.crypto.ECKeyPair;
//import org.web3j.crypto.Keys;
//
//import java.io.BufferedWriter;
//import java.io.FileWriter;
//import java.util.ArrayList;
//import java.util.List;
//
//public class EthereumWalletGenerator {
//
//    public static void main(String[] args) throws Exception {
//        int walletCount = 100; // 要生成的钱包数量
//        List<Wallet> wallets = generateWallets(walletCount);
//
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter("wallets.txt",  true))) {
//            // 打印生成的钱包信息
//            for (Wallet wallet : wallets) {
//                System.out.println("Private Key: " + wallet.getPrivateKey());
//                System.out.println("Address: " + wallet.getAddress());
//
//                writer.write(wallet.getPrivateKey() + " " + wallet.getAddress() + "\n");
//            }
//        }
//    }
//
//    public static List<Wallet> generateWallets(int count) throws Exception {
//        List<Wallet> walletList = new ArrayList<>();
//        for (int i = 0; i < count; i++) {
//            // 生成私钥对
//            ECKeyPair keyPair = Keys.createEcKeyPair();
//
//            // 获取私钥
//            String privateKey = keyPair.getPrivateKey().toString(16);
//
//            // 获取地址
//            String address = "0x" + Keys.getAddress(keyPair);
//
//            walletList.add(new Wallet(privateKey, address));
//        }
//        return walletList;
//    }
//
//    // 定义钱包类
//    public static class Wallet {
//        private final String privateKey;
//        private final String address;
//
//        public Wallet(String privateKey, String address) {
//            this.privateKey = privateKey;
//            this.address = address;
//        }
//
//        public String getPrivateKey() {
//            return privateKey;
//        }
//
//        public String getAddress() {
//            return address;
//        }
//    }
//}
