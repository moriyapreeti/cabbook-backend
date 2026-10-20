package org.cabbook.receipt;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import org.cabbook.auth.AuthUser;
import org.cabbook.common.ApiException;
import org.cabbook.payment.Payment;
import org.cabbook.payment.PaymentRepository;
import org.cabbook.payment.PaymentStatus;
import org.cabbook.ride.Ride;
import org.cabbook.ride.RideRepository;
import org.cabbook.ride.RideService;
import org.cabbook.ride.RideStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ReceiptService {
    private static final Logger log = LoggerFactory.getLogger(ReceiptService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final RideRepository rides;
    private final RideService rideService;
    private final PaymentRepository payments;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final String mailHost;

    public ReceiptService(RideRepository rides, RideService rideService, PaymentRepository payments,
            ObjectProvider<JavaMailSender> mailSender, @Value("${cabbook.mail.from}") String from,
            @Value("${spring.mail.host:}") String mailHost) {
        this.mailHost = mailHost;
        this.rides = rides;
        this.rideService = rideService;
        this.payments = payments;
        this.mailSender = mailSender;
        this.from = from;
    }

    public byte[] invoiceFor(AuthUser user, Long rideId) {
        Ride ride = rideService.load(rideId);
        rideService.assertCanView(user, ride);
        if (ride.getStatus() != RideStatus.COMPLETED)
            throw ApiException.conflict("Invoice is available after the ride is completed");
        return buildPdf(ride);
    }

    @Async
    public void emailReceiptAsync(Long rideId) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || mailHost.isBlank()) {
            log.info("Mail not configured, skipping receipt for ride {}", rideId);
            return;
        }
        try {
            Ride ride = rides.findDetailed(rideId).orElseThrow();
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(from);
            h.setTo(ride.getRider().getEmail());
            h.setSubject("Your CabBook receipt for ride #" + rideId);
            h.setText("Hi " + ride.getRider().getName()
                    + ",\n\nThanks for riding with CabBook. Your invoice is attached.\n");
            h.addAttachment("invoice-" + rideId + ".pdf", new ByteArrayResource(buildPdf(ride)));
            sender.send(msg);
        } catch (Exception e) {
            log.error("Could not email receipt for ride {}", rideId, e);
        }
    }

    byte[] buildPdf(Ride ride) {
        Payment pay = payments.findByRideId(ride.getId()).orElse(null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(doc, out);
        doc.open();

        doc.add(new Paragraph("CabBook - Ride Invoice", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20)));
        doc.add(new Paragraph("Invoice #" + ride.getId() + "   |   " + ride.getCompletedAt().format(FMT)));
        doc.add(Chunk.NEWLINE);

        PdfPTable t = new PdfPTable(new float[] { 1.2f, 3f });
        t.setWidthPercentage(100);
        row(t, "Rider", ride.getRider().getName());
        row(t, "Driver", ride.getDriver() == null ? "-" : ride.getDriver().getUser().getName());
        row(t, "Vehicle", ride.getDriver() == null ? "-"
                : ride.getDriver().getVehicleModel() + " (" + ride.getDriver().getVehicleNumber() + ")");
        row(t, "Pickup", ride.getPickupAddress());
        row(t, "Drop", ride.getDropAddress());
        row(t, "Distance", ride.getDistanceKm() + " km");
        row(t, "Duration", ride.getDurationMin() + " min");
        row(t, "Fare", "INR " + ride.getFare());
        row(t, "Discount",
                "INR " + ride.getDiscount() + (ride.getPromoCode() == null ? "" : " (" + ride.getPromoCode() + ")"));
        row(t, "Total", "INR " + ride.getPayable());
        row(t, "Payment", pay == null ? "Not paid"
                : (pay.getStatus() == PaymentStatus.PAID ? "Paid via " + pay.getProvider() : pay.getStatus().name()));
        doc.add(t);
        doc.close();
        return out.toByteArray();
    }

    private static void row(PdfPTable t, String k, String v) {
        PdfPCell a = new PdfPCell(new Phrase(k, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        PdfPCell b = new PdfPCell(new Phrase(v, FontFactory.getFont(FontFactory.HELVETICA, 11)));
        a.setPadding(6);
        b.setPadding(6);
        t.addCell(a);
        t.addCell(b);
    }
}
