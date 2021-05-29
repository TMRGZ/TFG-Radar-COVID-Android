package org.dpppt.android.sdk.internal.backend.models;



import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * A GaenKey is a Temporary Exposure Key of a person being infected, so it's also an Exposed Key. To
 * protect timing attacks, a key can be invalidated by the client by setting _fake_ to 1.
 */
public class GaenKey {
  public static final Integer GaenKeyDefaultRollingPeriod = 144;

  @NotNull
  @Size(min = 24, max = 24)
  //@Documentation(description = "Represents the 16-byte Temporary Exposure Key in base64")
  private String keyData;

  @NotNull

  private Integer rollingStartNumber;

  @NotNull

  private Integer rollingPeriod;

  @NotNull

  private Integer transmissionRiskLevel;


  private Integer fake = 0;


  private String countryOrigin;


  private Integer reportType;


  private Long daysSinceOnsetOfSymptons;


  private Boolean efgsSharing;
  

  private List<String> visitedCountries = new ArrayList<>();
  
  public GaenKey() {}

  public GaenKey(
      String keyData,
      Integer rollingStartNumber,
      Integer rollingPeriod,
      Integer transmissionRiskLevel,
      String countryOrigin,
      Integer reportType,
      Long daysSinceOnsetOfSymptons,
      Boolean efgsSharing,
      List<String> visitedCountries) {
    this.keyData = keyData;
    this.rollingStartNumber = rollingStartNumber;
    this.rollingPeriod = rollingPeriod;
    this.transmissionRiskLevel = transmissionRiskLevel;
    this.countryOrigin = countryOrigin;
    this.reportType = reportType;
    this.daysSinceOnsetOfSymptons = daysSinceOnsetOfSymptons;
    this.efgsSharing = efgsSharing;
    this.visitedCountries = visitedCountries;
  }

  public String getKeyData() {
    return this.keyData;
  }

  public void setKeyData(String keyData) {
    this.keyData = keyData;
  }

  public Integer getRollingStartNumber() {
    return this.rollingStartNumber;
  }

  public void setRollingStartNumber(Integer rollingStartNumber) {
    this.rollingStartNumber = rollingStartNumber;
  }

  public Integer getRollingPeriod() {
    return this.rollingPeriod;
  }

  public void setRollingPeriod(Integer rollingPeriod) {
    this.rollingPeriod = rollingPeriod;
  }

  public Integer getTransmissionRiskLevel() {
    return this.transmissionRiskLevel;
  }

  public void setTransmissionRiskLevel(Integer transmissionRiskLevel) {
    this.transmissionRiskLevel = transmissionRiskLevel;
  }

  public Integer getFake() {
    return this.fake;
  }

  public void setFake(Integer fake) {
    this.fake = fake;
  }

  public String getCountryOrigin() {
    return countryOrigin;
  }

  public void setCountryOrigin(String countryOrigin) {
    this.countryOrigin = countryOrigin;
  }

  public Integer getReportType() {
    return reportType;
  }

  public void setReportType(Integer reportType) {
    this.reportType = reportType;
  }

  public Long getDaysSinceOnsetOfSymptons() {
    return daysSinceOnsetOfSymptons;
  }

  public void setDaysSinceOnsetOfSymptons(Long daysSinceOnsetOfSymptons) {
    this.daysSinceOnsetOfSymptons = daysSinceOnsetOfSymptons;
  }

  public Boolean getEfgsSharing() {
    return efgsSharing;
  }

  public void setEfgsSharing(Boolean efgsSharing) {
    this.efgsSharing = efgsSharing;
  }

  public List<String> getVisitedCountries() {
	return visitedCountries;
  }

  public void setVisitedCountries(List<String> visitedCountries) {
	this.visitedCountries = visitedCountries;
  }

  @Override
  public String toString() {
    return "GaenKey{" +
            "keyData='" + keyData + '\'' +
            ", rollingStartNumber=" + rollingStartNumber +
            ", rollingPeriod=" + rollingPeriod +
            ", transmissionRiskLevel=" + transmissionRiskLevel +
            ", fake=" + fake +
            ", countryOrigin='" + countryOrigin + '\'' +
            ", reportType=" + reportType +
            ", daysSinceOnsetOfSymptons=" + daysSinceOnsetOfSymptons +
            ", efgsSharing=" + efgsSharing +
            ", visitedCountries=" + visitedCountries +
            '}';
  }

}
