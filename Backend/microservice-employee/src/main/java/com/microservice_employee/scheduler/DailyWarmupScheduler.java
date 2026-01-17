package com.microservice_employee.scheduler;

import com.microservice_employee.service.TalanaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import com.microservice_employee.dto.ContratoResumenDTO;

@Component
public class DailyWarmupScheduler {
    private static final Logger log = LoggerFactory.getLogger(DailyWarmupScheduler.class);

    private final TalanaService talanaService;

    public DailyWarmupScheduler(TalanaService talanaService) {
        this.talanaService = talanaService;
    }

    // Corre todos los días a las 5:00 AM hora Chile, precalienta la caché de contratos activos
    @Scheduled(cron = "0 0 5 * * *", zone = "America/Santiago")
    public void warmupContratosActivos() {
        try {
            log.info("[Scheduler] Warmup contratos activos iniciando (05:00 America/Santiago)");
            List<ContratoResumenDTO> list = talanaService.listarContratosActivos(null, null, true);
            log.info("[Scheduler] Warmup contratos activos OK: {} registros", list != null ? list.size() : 0);
        } catch (Exception ex) {
            log.warn("[Scheduler] Warmup contratos activos falló: {}", ex.getMessage());
        }
    }
}
