import service.BankingService;

//Entry point for the Banking App.
public class BankingApp {

    /**
     * @param args unused — the application takes all its input interactively
     *             via standard input, not command-line arguments
     */
    public static void main(String[] args) {
        BankingService bankingService = new BankingService();
        bankingService.start();
    }
}