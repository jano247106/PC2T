import java.util.ArrayList;
import java.util.Comparator;

abstract class Film {

    private int id;
    public String nazov;
    public String reziser;
    public int year;
    public ArrayList<Hodnotenie> zoznamHodnoteni;
    private boolean vymazany = false;

    public Film(int id, String nazov, String reziser, int year) {
        this.id = id;
        this.nazov = nazov;
        this.reziser = reziser;
        this.year = year;
        this.zoznamHodnoteni = new ArrayList<Hodnotenie>();
    }

    public boolean vymaz() {
        this.vymazany = true;
        return vymazany;
    }

    public void pridatHodnotenie(Hodnotenie hodnotenie) {
        this.zoznamHodnoteni.add(hodnotenie);
    }

    /**
     * @return Priemerné hodnotenie z celkových hodnotení užívateľmi
     */
    public float getPriemerneHodnotenie() {
        float celkovo = 0;
        float pocet = 0;

        for (Hodnotenie hodnotenie : this.zoznamHodnoteni) {
            celkovo += hodnotenie.stars;
            pocet++;
        }

        float priemer = celkovo / pocet;

        return !Float.isNaN(priemer) ? priemer : 0.0f;
    }

    public String getHodnotenia() {
        StringBuilder str = new StringBuilder("hodnotenia: \n");
        this.zoznamHodnoteni.sort(Comparator.comparingInt((Hodnotenie a) -> a.stars).reversed());

        for (Hodnotenie hodnotenie : this.zoznamHodnoteni) {
            str.append(hodnotenie.stars);
            str.append("\t");
            str.append(hodnotenie.hodnotenie);
            str.append("\n");
        }

        return str.toString();
    }

    public boolean isVymazany() { //Getter pre boolean
        return this.vymazany;
    }

    public String celyFilm() {
        return "Film";
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
