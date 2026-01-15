package me.shinsunyoung.projectweatherly.region.init;

import me.shinsunyoung.projectweatherly.region.domain.Region;
import me.shinsunyoung.projectweatherly.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class RegionDataInitializer implements CommandLineRunner {

    private final RegionRepository regionRepository;

    @Override
    public void run(String... args) throws Exception {

        if (regionRepository.count() > 0) {
            return;
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        getClass().getResourceAsStream("/data/legal_dong.txt"),
                        StandardCharsets.UTF_8
                )
        );

        String line;
        boolean firstLine = true;

        while ((line = reader.readLine()) != null) {

            // 헤더 스킵
            if (firstLine) {
                firstLine = false;
                continue;
            }

            String[] tokens = line.split("\t");
            if (tokens.length < 3) continue;

            String code = tokens[0].trim();
            String name = tokens[1].trim();
            boolean isActive = tokens[2].trim().equals("존재");

            regionRepository.save(
                    Region.builder()
                            .regionCode(code)
                            .regionName(name)
                            .active(isActive)
                            .nx(0)
                            .ny(0)
                            .build()
            );
        }
    }
}
