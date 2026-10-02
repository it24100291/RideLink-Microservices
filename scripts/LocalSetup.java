import java.nio.file.*;
import java.security.*;
import java.util.Base64;

/** Generates local development credentials outside the repository. Prints no secrets. */
class LocalSetup {
    public static void main(String[] args) throws Exception {
        Path home=Path.of(args[0]).toAbsolutePath();
        Files.createDirectories(home);
        Path privateKey=home.resolve("private.pem"), publicKey=home.resolve("public.pem");
        if(Files.exists(privateKey)!=Files.exists(publicKey))
            throw new IllegalStateException("Both private.pem and public.pem must exist together in "+home);
        if(!Files.exists(privateKey)){
            var generator=KeyPairGenerator.getInstance("RSA"); generator.initialize(2048);
            var pair=generator.generateKeyPair();
            Files.writeString(privateKey,pem("PRIVATE KEY",pair.getPrivate().getEncoded()),StandardOpenOption.CREATE_NEW);
            Files.writeString(publicKey,pem("PUBLIC KEY",pair.getPublic().getEncoded()),StandardOpenOption.CREATE_NEW);
        }
        for(String name:new String[]{"account-service.key","ride-service.key","payment-service.key"}){
            Path path=home.resolve(name);
            if(!Files.exists(path)){
                byte[] bytes=new byte[32]; new SecureRandom().nextBytes(bytes);
                Files.writeString(path,Base64.getUrlEncoder().withoutPadding().encodeToString(bytes),StandardOpenOption.CREATE_NEW);
            }
        }
        Files.createDirectories(home.resolve("data"));
        Files.createDirectories(home.resolve("logs"));
    }
    static String pem(String type,byte[] bytes){
        return "-----BEGIN "+type+"-----\n"+Base64.getMimeEncoder(64,new byte[]{'\n'}).encodeToString(bytes)+"\n-----END "+type+"-----\n";
    }
}
