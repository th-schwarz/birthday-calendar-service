package codes.thischwa.bcs.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the BCS application. These properties are mapped from configuration
 * sources with the prefix `bcs`.
 *
 * @param product The name of the product being configured.
 * @param calendarCategory The category of the calendar.
 * @param cron The cron expression for scheduling tasks.
 * @param runOnStart A flag indicating whether the associated task should run on application
 *     startup.
 */
@ConfigurationProperties(prefix = "bcs")
public record BcsConf(String product, String calendarCategory, String cron, boolean runOnStart) {

  public String getProdId() {
    return String.format("-//%s//iCal4j 1.0//EN", product);
  }
}
