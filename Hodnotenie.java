public class Hodnotenie {
    int id;
    String hodnotenie;
    int stars;

    public Hodnotenie(int stars, String slovneHodnotenie) {
        this.hodnotenie = slovneHodnotenie;
        this.stars = stars;
    }
}
