import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class AsenkronSistem {
    public static void main(String[] args) {
        ReservationSystem reservationSystem = new ReservationSystem();

        ReaderThread reader1 = new ReaderThread(reservationSystem, "Reader1");
        ReaderThread reader2 = new ReaderThread(reservationSystem, "Reader2");
        WriterThread writer1 = new WriterThread(reservationSystem, "Writer1");
        WriterThread writer2 = new WriterThread(reservationSystem, "Writer2");

        reader1.start();
        reader2.start();
        writer1.start();
        writer2.start();

        // Belirli bir süre çalışmasına izin ver
        try {
            Thread.sleep(5000); // 5 saniye bekle
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Tüm thread'leri durdur
        reader1.stopRunning();
        reader2.stopRunning();
        writer1.stopRunning();
        writer2.stopRunning();

        // Thread'lerin bitmesini bekle
        try {
            reader1.join();
            reader2.join();
            writer1.join();
            writer2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Program bitmeden önce koltuk durumlarını göster
        System.out.println("Son koltuk durumları: ");
        reservationSystem.printSeatStates();

        System.out.println("Program tamamlandı.");
    }
}

class ReservationSystem {
    private static List<Reservation> reservations;

    public ReservationSystem() {
        reservations = new ArrayList<>();
        for (char seat = 'A'; seat <= 'C'; seat++) {
            reservations.add(new Reservation(String.valueOf(seat)));
        }
    }

    public void printSeatStates() {
        System.out.println(getSeatStates());
    }

    public static String getSeatStates() {
        StringBuilder seatStates = new StringBuilder();
        for (Reservation reservation : reservations) {
            seatStates.append("Koltuk No ").append(reservation.getSeat()).append(" : ")
                    .append(reservation.isReserved() ? "1" : "0").append(" ");
        }
        return seatStates.toString();
    }

    public List<Reservation> getReservations() {
        return reservations;
    }
}

class Reservation {
    private String seat;
    private boolean isReserved;

    public Reservation(String seat) {
        this.seat = seat;
        this.isReserved = false;
    }

    public String getSeat() {
        return seat;
    }

    public boolean isReserved() {
        return isReserved;
    }

    public void reserve() {
        isReserved = true;
        log("Koltuk numarası " + seat + " başarıyla rezerve edildi.");
    }

    public void cancel() {
        isReserved = false;
        log("Koltuk " + seat + " için rezervasyon iptal edildi.");
    }

    public void query() {
        log("Koltuk " + seat + " sorgulandı: " + (isReserved ? "Rezerve" : "Müsait"));
    }

    private void log(String message) {
        System.out.println(getTimestamp() + " " + Thread.currentThread().getName() + " " + message);
    }

    private String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        return sdf.format(new Date());
    }
}

class ReaderThread extends Thread {
    private ReservationSystem reservationSystem;
    private volatile boolean running = true;

    public ReaderThread(ReservationSystem reservationSystem, String name) {
        super(name);
        this.reservationSystem = reservationSystem;
    }

    public void stopRunning() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            log("Müsait koltukları arıyor. Koltukların durumu:");
            StringBuilder state = new StringBuilder();
            for (Reservation reservation : reservationSystem.getReservations()) {
                state.append("Koltuk No ").append(reservation.getSeat()).append(" : ")
                        .append(reservation.isReserved() ? "1" : "0").append(" ");
                reservation.query();
            }
            log(state.toString().trim());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void log(String message) {
        System.out.println(getTimestamp() + " " + getName() + " " + message);
    }

    private String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        return sdf.format(new Date());
    }
}

class WriterThread extends Thread {
    private ReservationSystem reservationSystem;
    private volatile boolean running = true;

    public WriterThread(ReservationSystem reservationSystem, String name) {
        super(name);
        this.reservationSystem = reservationSystem;
    }

    public void stopRunning() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            for (Reservation reservation : reservationSystem.getReservations()) {
                log("Koltuk " + reservation.getSeat() + " için rezervasyon yapmayı deniyor.");
                reservation.reserve();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log("Koltuk " + reservation.getSeat() + " için rezervasyonu iptal etmeyi deniyor.");
                reservation.cancel();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void log(String message) {
        System.out.println(getTimestamp() + " " + getName() + " " + message);
    }

    private String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        return sdf.format(new Date());
    }
}
