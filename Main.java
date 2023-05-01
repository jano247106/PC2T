import java.io.*;
import java.nio.channels.SeekableByteChannel;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static ArrayList<Film> filmy = new ArrayList<Film>();
    public static ArrayList<Osoba> osoby = new ArrayList<Osoba>();
    public static String url = "jdbc:mysql://localhost:3306/filmiky";
    public static String username = "root";
    public static String password = "";

    public static void main(String[] args) throws Exception {
        ziskajUdajeDB();

        main_loop: while (true) {
            Scanner klavesnica = new Scanner(System.in);
            System.out.print("Vyber z nasledujucich možností:" +
                    "\na.............Pridanie filmu" +
                    "\nb.............Úprava filmu" +
                    "\nc.............Vymazanie filmu" +
                    "\nd.............Hodnotenie filmu" +
                    "\ne.............Výpis filmu" +
                    "\nf.............Vyhľadávanie filmu podla nazvu" +
                    "\ng.............Vypis hercov ktorí hraju vo viac ako jednom filme" +
                    "\nh.............Vyhladavanie filmov podla herca" +
                    "\ni.............Ukladanie filmu do suboru" +
                    "\nj.............Nacitanie filmu zo suboru" +
                    "\nx.............Ukoncenie aplikacie a ulozenie do databazy\n");
            switch (klavesnica.next().charAt(0)) {
                case 'A', 'a' -> {
                    pridajFilm();
                }
                case 'B', 'b' -> {
                    upravaFilmov();
                }
                case 'C', 'c' -> {
                    mazanieFilmov();
                }
                case 'D', 'd' -> {
                    hodnotenieFilmov();
                }
                case 'e', 'E' -> {
                    vypisFilmov();
                }
                case 'f', 'F' -> {
                    vyhladavanieFilmu();
                }
                case 'g', 'G' -> {
                    vypisViacAkoJedenFilm();
                }
                case 'h', 'H' -> {
                    vyhladatFilmyPodlaHerca("Vin Diesel");
                }
                case 'i', 'I' -> {
//                      ulozFilm();
                }
                case 'j', 'J' -> {
                    nacitajFilm();
                }
                case 'x', 'X' -> {
                    ulozUdajeDB();
                    break main_loop;
                }
            }
        }
    }

    public static void ziskajUdajeDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            Connection connection = DriverManager.getConnection(url, username, password);
            Statement personStmt = connection.createStatement();
            ResultSet personSet = personStmt.executeQuery("select * from persons");

            while (personSet.next()) {
                Osoba osoba;
                int id = personSet.getInt("id");
                String name = personSet.getString("name");
                String type = personSet.getString("type");

                switch (type) {
                    case "animator" -> {
                        osoba = new Animator(id, name);
                    }
                    case "actor" -> {
                        osoba = new Herec(id, name);
                    }
                    default -> {
                        System.err.println("Neznámy typ osoby");
                        continue;
                    }
                }

                osoby.add(osoba);
            }

            Statement filmyStmt = connection.createStatement();
            ResultSet filmySet = filmyStmt.executeQuery("select * from films");

            //Vypis z tabulky, SQL dopyt

            while (filmySet.next()) {
                Film film;
                int id = filmySet.getInt("id");
                String name = filmySet.getString("name");
                int year = filmySet.getInt("year");
                int age = filmySet.getInt("age");
                String reziser = filmySet.getString("dierector");
                String film_type = filmySet.getString("type");


                switch (film_type) {
                    case "animated" -> {
                        film = new AnimovanyFilm(id, name, reziser, year);
                        ((AnimovanyFilm) film).age = age;
                    }
                    case "live_action" -> {
                        film = new HranyFilm(id, name, reziser, year);
                    }
                    default -> throw new Exception("Invalid ENUM value.");
                }

                Connection connection1 = DriverManager.getConnection(url, username, password);
                Statement personStatement = connection1.createStatement();
                ResultSet osobaSet = personStatement.executeQuery("SELECT person_id, name, film_id, type FROM `person_film` AS pm LEFT JOIN persons AS p ON p.id = pm.person_id WHERE pm.film_id =" + id);

                while (osobaSet.next()) {
                    int osobaId = osobaSet.getInt("person_id");
                    Optional<Osoba> optionalOsoba = osoby.stream().filter(e -> e.getId() == osobaId).findFirst();
                    if (optionalOsoba.isEmpty()) {
                        System.err.println("Osoba nebola nájdená");
                        continue;
                    }
                    Osoba osoba = optionalOsoba.get();

                    if (osoba.filmy.stream().noneMatch(e -> e.getId() == film.getId())) {
                        osoba.filmy.add(film);
                    }

                    switch (film_type) {
                        case "animated" -> {
                            ((AnimovanyFilm) film).zoznamAnimatorov.add((Animator) osoba);
                        }
                        case "live_action" -> {
                            ((HranyFilm) film).zoznamHercov.add((Herec) osoba);
                        }
                        default -> throw new Exception("Invalid ENUM value.");
                    }
                }

                Statement hodnotenieStmt = connection.createStatement();
                ResultSet hodnotenieSet = hodnotenieStmt.executeQuery("select * from hodnotenie where film_id =" + id);

                while (hodnotenieSet.next()) {
                    String hodnotenie = hodnotenieSet.getString("hodnotenie");
                    int ratingStars = hodnotenieSet.getInt("stars");

                    film.pridatHodnotenie(new Hodnotenie(ratingStars, hodnotenie));
                }

                filmy.add(film);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void ulozUdajeDB() {
        try {
            Connection connection = DriverManager.getConnection(url, username, password);

            ArrayList<Osoba> noveOsoby = new ArrayList<>();

            for (Osoba osoba : osoby) {
                if (osoba.getId() == 0) {
                    int id = 0;
                    PreparedStatement personStmt = connection.prepareStatement("INSERT INTO persons (name, type) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
                    personStmt.setString(1, osoba.name);
                    personStmt.setString(2, osoba.getClass().getName().equals("Herec") ? "actor" : "animator");
                    personStmt.executeUpdate();

                    ResultSet generatedKeys = personStmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        id = generatedKeys.getInt(1);
                    }
                    osoba.setId(id);
                    noveOsoby.add(osoba);
                }
            }

            for (Film film : filmy) {
                int filmId = film.getId();
                if (filmId == 0) {
                    if (film.isVymazany()) {
                        continue;
                    }

                    PreparedStatement filmStmt = connection.prepareStatement("INSERT INTO films (name, dierector, year, type, age) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                    filmStmt.setString(1, film.nazov);
                    filmStmt.setString(2, film.reziser);
                    filmStmt.setInt(3, film.year);
                    filmStmt.setString(4, film.getClass().getName().equals("AnimovanyFilm") ? "animated" : "live_action");
                    if (film.getClass().getName().equals("AnimovanyFilm")) {
                        filmStmt.setInt(5, ((AnimovanyFilm) film).age);
                    } else {
                        filmStmt.setInt(5, 0);
                    }
                    filmStmt.executeUpdate();

                    ResultSet generatedKeys = filmStmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        filmId = generatedKeys.getInt(1);
                    }

                    film.setId(filmId);
                } else {
                    if (!film.isVymazany()) {
                        PreparedStatement filmStmt = connection.prepareStatement("UPDATE films SET name = ?, dierector = ?, year = ?, type = ?, age = ? WHERE id = ?");
                        filmStmt.setString(1, film.nazov);
                        filmStmt.setString(2, film.reziser);
                        filmStmt.setInt(3, film.year);
                        filmStmt.setString(4, film.getClass().getName().equals("AnimovanyFilm") ? "animated" : "live_action");
                        filmStmt.setInt(5, film.year);
                        filmStmt.setInt(6, film.getId());
                        filmStmt.executeUpdate();
                    } else {
                        PreparedStatement hodnotenieStmt = connection.prepareStatement("DELETE FROM hodnotenie WHERE film_id = ?");
                        hodnotenieStmt.setInt(1, film.getId());
                        hodnotenieStmt.execute();

                        PreparedStatement personFilmStmt = connection.prepareStatement("DELETE FROM person_film WHERE film_id = ?");
                        personFilmStmt.setInt(1, film.getId());
                        personFilmStmt.execute();

                        PreparedStatement filmStmt = connection.prepareStatement("DELETE FROM films WHERE id = ?");
                        filmStmt.setInt(1, film.getId());
                        filmStmt.execute();
                    }
                }

                for (Hodnotenie hodnotenie : film.zoznamHodnoteni) {
                    if (hodnotenie.id == 0) {
                        PreparedStatement hodnotenieStmt = connection.prepareStatement("INSERT INTO hodnotenie (film_id, stars, hodnotenie) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                        hodnotenieStmt.setInt(1, filmId);
                        hodnotenieStmt.setInt(2, hodnotenie.stars);
                        hodnotenieStmt.setString(3, hodnotenie.hodnotenie);
                        hodnotenieStmt.executeUpdate();
                    }
                }
            }

            for (Osoba osoba : osoby) {
                for (Film film : osoba.filmy) {
                    PreparedStatement personFilmStmt = connection.prepareStatement("INSERT IGNORE INTO person_film (film_id, person_id) VALUES (?, ?)");
                    personFilmStmt.setInt(1, film.getId());
                    personFilmStmt.setInt(2, osoba.getId());
                    personFilmStmt.executeUpdate();

                }
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    public static void vypisViacAkoJedenFilm() {
        List<Osoba> listOsob = osoby.stream().filter(e -> e.filmy.size() > 1).toList();

        for (Osoba osoba : listOsob) {
            System.out.println(osoba);
            System.out.println(osoba.getZoznamFilmov()+"\n");
        }
    }

    public static void pridajFilm() {
        int typFilmu = 0;
        String nazovFilmu;
        String reziser;
        int rokVydania = 0;
        int age = 0;
        ArrayList<Herec> herci = new ArrayList<Herec>();
        ArrayList<Animator> animatori = new ArrayList<Animator>();

        Scanner klavesnica = new Scanner(System.in);

        loop:
        while (true) {
            System.out.print("Vyber druh filmu: \n1- hrany 2 - animovany\n-> ");
            typFilmu = klavesnica.nextInt();
            switch (typFilmu) {
                case 1 -> {
                    System.out.println("Zvoleny hrany film");
                    break loop;
                }
                case 2 -> {
                    System.out.println("Zvoleny animovany film");
                    break loop;
                }
                default -> System.out.println("Nespravne zvoleny druh filmu (1 / 2");
            }
        }
        klavesnica.nextLine();
        System.out.print("Zadaj nazov filmu: ");
        nazovFilmu = klavesnica.nextLine();
        System.out.print("Zadaj meno rezisera: ");
        reziser = klavesnica.nextLine();
        System.out.print("Zadaj rok vydania: ");
        rokVydania = klavesnica.nextInt();
        System.out.println("Zadaj Hercov/Animatorov.");
        System.out.println("Meno potvrd klavesou ENTER\nPo zadani posledneho uzivatela, potvrd klavesou ENTER");
        klavesnica.nextLine();

        while (true) {
            String menoOsoby = klavesnica.nextLine();

            if (menoOsoby.equals("")) {
                break;
            }

            Optional<Osoba> optionalOsoba = osoby.stream().filter(e -> e.name.equals(menoOsoby)).findFirst();

            Osoba osoba = null;
            if (optionalOsoba.isPresent()) {
                osoba = optionalOsoba.get();
            }

            switch (typFilmu) {
                case 1 -> {
                    if (osoba == null) {
                        osoba = new Herec(0, menoOsoby);
                        osoby.add(osoba);
                    }

                    herci.add((Herec) osoba);
                }
                case 2 -> {
                    if (osoba == null) {
                        osoba = new Animator(0, menoOsoby);
                        osoby.add(osoba);
                    }
                    animatori.add((Animator) osoba);
                }
            }
        }

        switch (typFilmu) {
            case 1 -> {
                HranyFilm hranyFilm = new HranyFilm(0, nazovFilmu, reziser, rokVydania);
                hranyFilm.zoznamHercov = herci;
                filmy.add(hranyFilm);
            }

            case 2 -> {
                System.out.println("Zadaj odporucany vek: ");
                age = klavesnica.nextInt();
                AnimovanyFilm animovanyFilm = new AnimovanyFilm(0, nazovFilmu, reziser, rokVydania, age);
                animovanyFilm.zoznamAnimatorov = animatori;
                filmy.add(animovanyFilm);
            }
        }
    }

    //---------------
    public static void upravaFilmov() {
        System.out.println("Zadaj nazov filmu ktory chces upravit: ");
        Scanner klavesnica = new Scanner(System.in);
        String nazov = klavesnica.nextLine()+".txt";
        for (Film film : filmy) {
            if (nazov.equals(film.nazov)) {

            }
        }
    }

    public static void vyhladavanieFilmu() {
        Scanner klavesnica = new Scanner(System.in);
        System.out.print("Zadaj nazov filmu: ");
        String nazovFilmu = klavesnica.nextLine();

        Film film = vyhladajFilmPodlaMena(nazovFilmu);

        if (film == null) {
            System.err.println("Film nebol nájdený");
            return;
        }

        if(film.isVymazany()){
            System.out.println("Film bol vymazany");
        }
        
        System.out.println(film.celyFilm());
        System.out.println(film.getHodnotenia());
    }

    public static void mazanieFilmov() {
        System.out.println("Zadaj nazov filmu ktory chces vymazat.");
        Scanner klavesnica = new Scanner(System.in);
        String nazov = klavesnica.nextLine();
        Film film = vyhladajFilmPodlaMena(nazov);
        if (film == null) {
            System.out.println("Film sa nenasiel");
            return;
        }
        film.vymaz();
        System.out.println("Film bol vymazany");
    }

    public static void hodnotenieFilmov() throws Exception {
        String nazov;
        Scanner klavesnica = new Scanner(System.in);
        System.out.print("Zadaj nazov filmu: ");
        nazov = klavesnica.nextLine();
        Film film = vyhladajFilmPodlaMena(nazov);
        if (film == null) {
            System.out.println("Film nenájdený");
            return;
        }

        switch (film.getClass().getName()) {
            case "HranyFilm" -> {
                System.out.print("Hrany filmik\nZadajte hodnotenie od 1 - 5:");
                int stars = klavesnica.nextInt();
                klavesnica.nextLine();
                System.out.print("Slovne hodnotenie: ");
                String hodnotenieSlovne = klavesnica.nextLine();

                Hodnotenie hodnotenie = new Hodnotenie(stars, hodnotenieSlovne);
                film.pridatHodnotenie(hodnotenie);
            }
            case "AnimovanyFilm" -> {
                System.out.print("Animak\nZadajte hodnotenie od 1 - 10:");
                int rating = klavesnica.nextInt();
                klavesnica.nextLine();
                System.out.print("Slovne hodnotenie: ");
                String hodnotenieSlovne = klavesnica.nextLine();

                Hodnotenie hodnotenie = new Hodnotenie(rating, hodnotenieSlovne);
                film.pridatHodnotenie(hodnotenie);
            }
            default -> {
                throw new Exception("Nedá sa svietiť");
            }
        }
    }

    public static void ulozFilm(Film film) {
        String nazovSuboru = film.nazov + ".txt";
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(nazovSuboru, false));
            switch (film.getClass().getName()) {
                case "HranyFilm" -> {
                    writer.append("name;dierector;year;type");
                    writer.append(String.format("%s;%s;%d;%s", film.nazov, film.reziser, film.year, "live_action"));
                }
                case "AnimovanyFilm" -> {
                    writer.append("name;dierector;year;type;age");
                    writer.append(String.format("%s;%s;%d;%s;%d", film.nazov, film.reziser, film.year, "animated", ((AnimovanyFilm) film).age));
                }
                default -> {
                    System.err.println("Nepodarilo sa uložiť film (neznámy typ filmu)");
                    return;
                }
            }
            writer.close();
        } catch (Exception e) {
            System.err.println("Nepodarilo sa uložiť súbor");
            return;
        }
        System.out.printf("Súbor bol uložený ako: \"%s\"", nazovSuboru);
    }

    public static void nacitajFilm() {
        System.out.print("Zadaj nazov filmu (suboru): ");
        Scanner klavesnica = new Scanner(System.in);
        String nazovSuboru = klavesnica.nextLine()+".txt";

        try {
            BufferedReader reader = new BufferedReader(new FileReader(nazovSuboru));
            List<String> lines = reader.lines().toList();

            if (lines.size() != 2) {
                System.err.println("Súbor je poškodený");
            }

            String[] hodnoty = lines.get(1).split(";");

            Film film;

            switch (hodnoty[3]) {
                case "animated" -> {
                    film = new AnimovanyFilm(0, hodnoty[0], hodnoty[1], Integer.parseInt(hodnoty[2]), Integer.parseInt(hodnoty[4]));
                }
                case "live_action" -> {
                    film = new HranyFilm(0, hodnoty[0], hodnoty[1], Integer.parseInt(hodnoty[2]));
                }
                default -> {
                    System.err.println("Neznámy typ filmu v súbore");
                    return;
                }
            }

            System.out.println("Súbor obsahuje film:");
            System.out.println(film);
        } catch (IndexOutOfBoundsException e) {
            System.err.println("Súbor je poškodený");
        } catch (Exception e) {
            System.err.println("Nepodarilo sa otvoriť/prečítať súbor");
        }
    }

    public static ArrayList<Film> vyhladatFilmyPodlaHerca(String menoOsoby) {
        Optional<Osoba> optionalOsoba = osoby.stream().filter(e -> e.name.equals(menoOsoby)).findFirst();

        if (optionalOsoba.isEmpty()) {
            System.err.println("Osoba nenájdená");
            return null;
        }

        Osoba osoba = optionalOsoba.get();

        System.out.println("Filmy herca "+osoba+": ");
        System.out.println();

        return osoba.filmy;

    }

    public static Film vyhladajFilmPodlaMena(String nazov) {
        for (Film film : filmy) {
            if (film.isVymazany()) {
                continue;
            }
            if ((film.nazov).equals(nazov)) {
                return film;
            }
        }
        return null;
    }

    public static void vypisFilmov() {
        System.out.println("Zoznam vsetkych filmov: ");
        for (Film film : filmy) {
            System.out.println(film.celyFilm());
        }
    }


}











