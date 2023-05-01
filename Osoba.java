import java.util.ArrayList;

public class Osoba {
    private int id;
    public String name;

    public ArrayList<Film> filmy = new ArrayList<Film>();

    public Osoba(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getZoznamFilmov() {
        StringBuilder str = new StringBuilder("filmy:\n");

        for (Film film : this.filmy) {
            str.append(film.toString());
        }

        return str.toString();
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
       this.id = id;
    }
}
