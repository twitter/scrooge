/*
 * Common constants used by ad projects
 *
 */

// namespace.
namespace java com.twitter.adserver
namespace rb Ads
namespace py gen.twitter.adserver.adserver_common_constants

const string REGION_EUROPE = "EUROPE"
const string REGION_ASIA = "ASIA"
const string REGION_NORTH_AMERICA = "N_AMERICA"
const string REGION_SOUTH_AMERICA = "S_AMERICA"
const string REGION_OCEANIA = "OCEANIA"
const string REGION_AFRICA = "AFRICA"
const string REGION_REST_OF_WORLD = "REST_OF_WORLD"

const map<string, string> COUNTRY_REGION_MAP = {
  "EU" : REGION_EUROPE,
  "AD" : REGION_EUROPE,
  "AE" : REGION_ASIA,
  "AF" : REGION_ASIA,
  "AG" : REGION_SOUTH_AMERICA,
  "AI" : REGION_SOUTH_AMERICA,
  "AL" : REGION_EUROPE,
  "AM" : REGION_ASIA,
  "AN" : REGION_SOUTH_AMERICA,
  "AO" : REGION_AFRICA,
  //"AQ" : REGION_ANTARCTICA, // This will roll into REST_OF_WORLD
  "AR" : REGION_SOUTH_AMERICA,
  "AS" : REGION_OCEANIA,
  "AT" : REGION_EUROPE,
  "AU" : REGION_OCEANIA,
  "AW" : REGION_SOUTH_AMERICA,
  "AX" : REGION_EUROPE,
  "AZ" : REGION_ASIA,
  "BA" : REGION_EUROPE,
  "BB" : REGION_SOUTH_AMERICA,
  "BD" : REGION_ASIA,
  "BE" : REGION_EUROPE,
  "BF" : REGION_AFRICA,
  "BG" : REGION_EUROPE,
  "BH" : REGION_ASIA,
  "BI" : REGION_AFRICA,
  "BJ" : REGION_AFRICA,
  "BM" : REGION_SOUTH_AMERICA,
  "BN" : REGION_ASIA,
  "BO" : REGION_SOUTH_AMERICA,
  "BR" : REGION_SOUTH_AMERICA,
  "BS" : REGION_SOUTH_AMERICA,
  "BT" : REGION_ASIA,
  "BV" : REGION_AFRICA,
  "BW" : REGION_AFRICA,
  "BY" : REGION_EUROPE,
  "BZ" : REGION_SOUTH_AMERICA,
  "CA" : REGION_NORTH_AMERICA,
  "CC" : REGION_ASIA,
  "CD" : REGION_AFRICA,
  "CF" : REGION_AFRICA,
  "CG" : REGION_AFRICA,
  "CH" : REGION_EUROPE,
  "CI" : REGION_AFRICA,
  "CK" : REGION_OCEANIA,
  "CL" : REGION_SOUTH_AMERICA,
  "CM" : REGION_AFRICA,
  "CN" : REGION_ASIA,
  "CO" : REGION_SOUTH_AMERICA,
  "CR" : REGION_SOUTH_AMERICA,
  "CU" : REGION_SOUTH_AMERICA,
  "CV" : REGION_AFRICA,
  "CX" : REGION_ASIA,
  "CY" : REGION_ASIA,
  "CZ" : REGION_EUROPE,
  "DE" : REGION_EUROPE,
  "DJ" : REGION_AFRICA,
  "DK" : REGION_EUROPE,
  "DM" : REGION_SOUTH_AMERICA,
  "DO" : REGION_SOUTH_AMERICA,
  "DZ" : REGION_AFRICA,
  "EC" : REGION_SOUTH_AMERICA,
  "EE" : REGION_EUROPE,
  "EG" : REGION_AFRICA,
  "EH" : REGION_AFRICA,
  "ER" : REGION_AFRICA,
  "ES" : REGION_EUROPE,
  "ET" : REGION_AFRICA,
  "FI" : REGION_EUROPE,
  "FJ" : REGION_OCEANIA,
  "FK" : REGION_SOUTH_AMERICA,
  "FM" : REGION_OCEANIA,
  "FO" : REGION_EUROPE,
  "FR" : REGION_EUROPE,
  "FX" : REGION_EUROPE,
  "GA" : REGION_AFRICA,
  "GB" : REGION_EUROPE,
  "GD" : REGION_SOUTH_AMERICA,
  "GE" : REGION_ASIA,
  "GF" : REGION_SOUTH_AMERICA,
  "GG" : REGION_EUROPE,
  "GH" : REGION_AFRICA,
  "GI" : REGION_EUROPE,
  "GL" : REGION_SOUTH_AMERICA,
  "GM" : REGION_AFRICA,
  "GN" : REGION_AFRICA,
  "GP" : REGION_SOUTH_AMERICA,
  "GQ" : REGION_AFRICA,
  "GR" : REGION_EUROPE,
  "GS" : REGION_SOUTH_AMERICA,
  "GT" : REGION_SOUTH_AMERICA,
  "GU" : REGION_OCEANIA,
  "GW" : REGION_AFRICA,
  "GY" : REGION_SOUTH_AMERICA,
  "HK" : REGION_ASIA,
  "HM" : REGION_AFRICA,
  "HN" : REGION_SOUTH_AMERICA,
  "HR" : REGION_EUROPE,
  "HT" : REGION_SOUTH_AMERICA,
  "HU" : REGION_EUROPE,
  "ID" : REGION_ASIA,
  "IE" : REGION_EUROPE,
  "IL" : REGION_ASIA,
  "IM" : REGION_EUROPE,
  "IN" : REGION_ASIA,
  "IO" : REGION_ASIA,
  "IQ" : REGION_ASIA,
  "IR" : REGION_ASIA,
  "IS" : REGION_EUROPE,
  "IT" : REGION_EUROPE,
  "JE" : REGION_EUROPE,
  "JM" : REGION_SOUTH_AMERICA,
  "JO" : REGION_ASIA,
  "JP" : REGION_ASIA,
  "KE" : REGION_AFRICA,
  "KG" : REGION_ASIA,
  "KH" : REGION_ASIA,
  "KI" : REGION_OCEANIA,
  "KM" : REGION_AFRICA,
  "KN" : REGION_SOUTH_AMERICA,
  "KP" : REGION_ASIA,
  "KR" : REGION_ASIA,
  "KW" : REGION_ASIA,
  "KY" : REGION_SOUTH_AMERICA,
  "KZ" : REGION_ASIA,
  "LA" : REGION_ASIA,
  "LB" : REGION_ASIA,
  "LC" : REGION_SOUTH_AMERICA,
  "LI" : REGION_EUROPE,
  "LK" : REGION_ASIA,
  "LR" : REGION_AFRICA,
  "LS" : REGION_AFRICA,
  "LT" : REGION_EUROPE,
  "LU" : REGION_EUROPE,
  "LV" : REGION_EUROPE,
  "LY" : REGION_AFRICA,
  "MA" : REGION_AFRICA,
  "MC" : REGION_EUROPE,
  "MD" : REGION_EUROPE,
  "MG" : REGION_AFRICA,
  "MH" : REGION_OCEANIA,
  "MK" : REGION_EUROPE,
  "ML" : REGION_AFRICA,
  "MM" : REGION_ASIA,
  "MN" : REGION_ASIA,
  "MO" : REGION_ASIA,
  "MP" : REGION_OCEANIA,
  "MQ" : REGION_SOUTH_AMERICA,
  "MR" : REGION_AFRICA,
  "MS" : REGION_SOUTH_AMERICA,
  "MT" : REGION_EUROPE,
  "MU" : REGION_AFRICA,
  "MV" : REGION_ASIA,
  "MW" : REGION_AFRICA,
  "MX" : REGION_NORTH_AMERICA,
  "MY" : REGION_ASIA,
  "MZ" : REGION_AFRICA,
  "NA" : REGION_AFRICA,
  "NC" : REGION_OCEANIA,
  "NE" : REGION_AFRICA,
  "NF" : REGION_OCEANIA,
  "NG" : REGION_AFRICA,
  "NI" : REGION_SOUTH_AMERICA,
  "NL" : REGION_EUROPE,
  "NO" : REGION_EUROPE,
  "NP" : REGION_ASIA,
  "NR" : REGION_OCEANIA,
  "NU" : REGION_OCEANIA,
  "NZ" : REGION_OCEANIA,
  "OM" : REGION_ASIA,
  "PA" : REGION_SOUTH_AMERICA,
  "PE" : REGION_SOUTH_AMERICA,
  "PF" : REGION_OCEANIA,
  "PG" : REGION_OCEANIA,
  "PH" : REGION_ASIA,
  "PK" : REGION_ASIA,
  "PL" : REGION_EUROPE,
  "PM" : REGION_SOUTH_AMERICA,
  "PN" : REGION_OCEANIA,
  "PR" : REGION_SOUTH_AMERICA,
  "PS" : REGION_ASIA,
  "PT" : REGION_EUROPE,
  "PW" : REGION_OCEANIA,
  "PY" : REGION_SOUTH_AMERICA,
  "QA" : REGION_ASIA,
  "RE" : REGION_AFRICA,
  "RO" : REGION_EUROPE,
  "RU" : REGION_ASIA,
  "RW" : REGION_AFRICA,
  "SA" : REGION_ASIA,
  "SB" : REGION_OCEANIA,
  "SC" : REGION_AFRICA,
  "SD" : REGION_AFRICA,
  "SE" : REGION_EUROPE,
  "SG" : REGION_ASIA,
  "SH" : REGION_AFRICA,
  "SI" : REGION_EUROPE,
  "SJ" : REGION_EUROPE,
  "SK" : REGION_EUROPE,
  "SL" : REGION_AFRICA,
  "SM" : REGION_EUROPE,
  "SN" : REGION_AFRICA,
  "SO" : REGION_AFRICA,
  "SR" : REGION_SOUTH_AMERICA,
  "ST" : REGION_AFRICA,
  "SV" : REGION_SOUTH_AMERICA,
  "SY" : REGION_ASIA,
  "SZ" : REGION_AFRICA,
  "TC" : REGION_SOUTH_AMERICA,
  "TD" : REGION_AFRICA,
  "TF" : REGION_AFRICA,
  "TG" : REGION_AFRICA,
  "TH" : REGION_ASIA,
  "TJ" : REGION_ASIA,
  "TK" : REGION_OCEANIA,
  "TM" : REGION_ASIA,
  "TN" : REGION_AFRICA,
  "TO" : REGION_OCEANIA,
  "TP" : REGION_ASIA,
  "TR" : REGION_ASIA,
  "TT" : REGION_SOUTH_AMERICA,
  "TV" : REGION_OCEANIA,
  "TW" : REGION_ASIA,
  "TZ" : REGION_AFRICA,
  "UA" : REGION_EUROPE,
  "UG" : REGION_AFRICA,
  "UM" : REGION_OCEANIA,
  "US" : REGION_NORTH_AMERICA,
  "UY" : REGION_SOUTH_AMERICA,
  "UZ" : REGION_ASIA,
  "VA" : REGION_EUROPE,
  "VC" : REGION_SOUTH_AMERICA,
  "VE" : REGION_SOUTH_AMERICA,
  "VG" : REGION_SOUTH_AMERICA,
  "VI" : REGION_SOUTH_AMERICA,
  "VN" : REGION_ASIA,
  "VU" : REGION_OCEANIA,
  "WF" : REGION_OCEANIA,
  "WS" : REGION_OCEANIA,
  "YE" : REGION_ASIA,
  "YT" : REGION_AFRICA,
  "YU" : REGION_EUROPE,
  "ZA" : REGION_AFRICA,
  "ZM" : REGION_AFRICA,
  "ZR" : REGION_AFRICA,
  "ZW" : REGION_AFRICA,

  // Fix deprecated UK labels
  "UK" : "GB",

  // Top 50 countries by daily user logins in the 4 weeks before 2012-12-05
  "US" : "US",  // United States
  "BR" : "BR",  // Brazil
  "ID" : "ID",  // Indonesia
  "GB" : "GB",  // United Kingdom
  "JP" : "JP",  // Japan
  "IN" : "IN",  // India
  "MX" : "MX",  // Mexico
  "TR" : "TR",  // Turkey
  "PH" : "PH",  // Philippines
  "KR" : "KR",  // Korea, Republic of

  "CA" : "CA",  // Canada
  "ES" : "ES",  // Spain
  "AR" : "AR",  // Argentina
  "SA" : "SA",  // Saudi Arabia
  "CO" : "CO",  // Colombia
  "FR" : "FR",  // France
  "RU" : "RU",  // Russian Federation
  "VE" : "VE",  // Venezuela, Bolivarian Republic of
  "IT" : "IT",  // Italy
  "TH" : "TH",  // Thailand

  "DE" : "DE",  // Germany
  "MY" : "MY",  // Malaysia
  "NL" : "NL",  // Netherlands
  "AU" : "AU",  // Australia
  "CL" : "CL",  // Chile
  "EG" : "EG",  // Egypt
  "PE" : "PE",  // Peru
  "PK" : "PK",  // Pakistan
  "ZA" : "ZA",  // South Africa
  "NG" : "NG",  // Nigeria

  "AP" : "AP",  // unknown country
  "UA" : "UA",  // Ukraine
  "AE" : "AE",  // United Arab Emirates
  "EC" : "EC",  // Ecuador
  "PL" : "PL",  // Poland
  "KW" : "KW",  // Kuwait
  "VN" : "VN",  // Viet Nam
  "SG" : "SG",  // Singapore
  "SE" : "SE",  // Sweden
  "DZ" : "DZ",  // Algeria

  "MA" : "MA",  // Morocco
  "TW" : "TW",  // Taiwan, Province of China
  "BE" : "BE",  // Belgium
  "DO" : "DO",  // Dominican Republic
  "IE" : "IE",  // Ireland
  "NO" : "NO",  // Norway
  "RO" : "RO",  // Romania
  "GR" : "GR",  // Greece
  "IL" : "IL",  // Israel
  "KZ" : "KZ"  // Kazakhstan
}
