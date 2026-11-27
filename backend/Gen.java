import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class Gen {
  public static void main(String[] args) {
    BCryptPasswordEncoder e = new BCryptPasswordEncoder();
    System.out.println("admin123=" + e.encode("admin123"));
    System.out.println("cs123456=" + e.encode("cs123456"));
  }
}
