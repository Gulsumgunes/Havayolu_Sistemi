import java.util.*;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.swing.*;
import java.awt.*;

class Main3 {
    public static void main(String[] args) {
        ReservationSystem3 reservationSystem = new ReservationSystem3();
        GUI gui = new GUI();

        ReaderThread3 reader1 = new ReaderThread3(reservationSystem, gui, "Reader1");
        ReaderThread3 reader2 = new ReaderThread3(reservationSystem, gui, "Reader2");
        WriterThread3 writer1 = new WriterThread3(reservationSystem, gui, "Writer1");
        WriterThread3 writer2 = new WriterThread3(reservationSystem, gui, "Writer2");

        reader1.start();
        reader2.start();
        writer1.start();
        writer2.start();

        // Belirli bir süre çalışmasını sağla
        try {
            Thread.sleep(5000);  // 5 saniye bekle
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

         // Program sonlanmadan hemen önce koltuk durumlarını göster
        System.out.println("Son koltuk durumları: ");
        reservationSystem.printSeatStates();

        System.out.println("Program tamamlandı.");
    }
}

class GUI extends JFrame {
    private Map<String, JLabel> seatLabels;

    public GUI() {
        setTitle("Havayolu Rezervasyon Sistemi");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(2, 5));

        seatLabels = new HashMap<>();
        for (char seat = 'A'; seat <= 'J'; seat++) {
            JLabel seatLabel = new JLabel(seat + ": Müsait");
            seatLabel.setHorizontalAlignment(SwingConstants.CENTER);
            seatLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            seatLabels.put(String.valueOf(seat), seatLabel);
            add(seatLabel);
        }

        setVisible(true);
    }

    public void updateSeatStatus(Reservation3 reservation) {
        SwingUtilities.invokeLater(() -> {
            String seat = reservation.getSeat();
            boolean isReserved = reservation.isReserved();
            JLabel seatLabel = seatLabels.get(seat);
            seatLabel.setText(seat + ": " + (isReserved ? "Rezerve" : "Müsait"));
            seatLabel.setBackground(isReserved ? Color.RED : Color.GREEN);
            seatLabel.setOpaque(true);
        });
    }
}

class ReservationSystem3 {
    private static List<Reservation3> reservations;

    public ReservationSystem3() {
        reservations = new ArrayList<>();
        for (char seat = 'A'; seat <= 'J'; seat++) {
            reservations.add(new Reservation3(String.valueOf(seat)));
        }
    }

    public List<Reservation3> getReservations() {
        return reservations;
    }

    public static String getSeatStates() {
        StringBuilder seatStates = new StringBuilder();
        for (Reservation3 reservation : reservations) {
            seatStates.append("Koltuk No ").append(reservation.getSeat()).append(" : ")
                      .append(reservation.isReserved() ? "1" : "0").append(" ");
        }
        return seatStates.toString();
    }

    public void printSeatStates() {
        System.out.println(getSeatStates());
    }
}

class Reservation3 {
    private String seat;
    private boolean isReserved;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true); // Adil kilit

    public Reservation3(String seat) {
        this.seat = seat;
        this.isReserved = false;
    }

    public String getSeat() {
        return seat;
    }

    public boolean isReserved() {
        return isReserved;
    }

    public void reserve(String threadName) {
        lock.writeLock().lock();
        try {
            log(threadName + " koltuğu rezerve etmeye çalışıyor: " + seat);
            if (!isReserved) {
                isReserved = true;
                log("Writer koltuk numarası " + seat + " başarıyla rezerve edildi.");
            } else {
                log("Writer koltuk numarası " + seat + " zaten rezerve edilmiş durumda.");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void cancel(String threadName) {
        lock.writeLock().lock();
        try {
            log(threadName + " koltuk rezervasyonunu iptal etmeye çalışıyor: " + seat);
            if (isReserved) {
                isReserved = false;
                log("Writer koltuk rezervasyonu iptal edildi: " + seat);
            } else {
                log("Writer koltuk rezervasyonu iptal edilemedi, çünkü zaten rezerve edilmemiş: " + seat);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void query(String threadName) {
        lock.readLock().lock();
        try {
            log(threadName + " müsait koltukları arıyor. Koltuk durumu:\n" + ReservationSystem3.getSeatStates());
        } finally {
            lock.readLock().unlock();
        }
    }

    private void log(String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        String time = sdf.format(new Date());
        System.out.println("Zaman: " + time + " - " + message);
        System.out.println();
    }

}

class ReaderThread3 extends Thread {
    private ReservationSystem3 reservationSystem;
    private volatile boolean running = true;
    private GUI gui;

    public ReaderThread3(ReservationSystem3 reservationSystem, GUI gui, String string) {
        this.reservationSystem = reservationSystem;
        this.gui = gui;
    }

    public void stopRunning() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            for (Reservation3 reservation : reservationSystem.getReservations()) {
                reservation.query(getName());
                gui.updateSeatStatus(reservation);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

class WriterThread3 extends Thread {
    private ReservationSystem3 reservationSystem;
    private volatile boolean running = true;
    private GUI gui;

    public WriterThread3(ReservationSystem3 reservationSystem, GUI gui, String string) {
        this.reservationSystem = reservationSystem;
        this.gui = gui;
    }

    public void stopRunning() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            for (Reservation3 reservation : reservationSystem.getReservations()) {
                if (Math.random() < 0.5) {
                    reservation.reserve(getName());
                } else {
                    reservation.cancel(getName());
                }
                gui.updateSeatStatus(reservation);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
