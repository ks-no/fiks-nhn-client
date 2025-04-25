# NHN client
Used for communicating with NHN

# Notes
- When using a DPoP access token in the `Authorization` header, it must be prefixed with `DPoP` instead of `Bearer`
- The DPoP proof is put in the `DPoP` header without any prefix, and must contain an encoded hash of the used access token

# Documentation

DPoP (Demonstrating Proof of Possession at the Application Layer): https://utviklerportal.nhn.no/informasjonstjenester/helseid/bruksmoenstre-og-eksempelkode/bruk-av-helseid/docs/dpop/dpop_enmd

Hodemelding: https://sarepta.helsedir.no/standard/Hodemelding  
Dialogmelding 1.0: https://sarepta.helsedir.no/standard/Dialogmelding/1.0  
Dialogmelding 1.1: https://sarepta.helsedir.no/standard/Dialogmelding/1.1  
Vedlegg til meldinger: https://sarepta.helsedir.no/standard/Vedlegg%20til%20meldinger  

Kodeverk: https://finnkode.helsedirektoratet.no/adm/collections

HIS 1174:2016: https://www.helsedirektoratet.no/standarder/bruk-av-kontaktopplysninger-i-basismeldinger-dialogmelding-og-pleie-og-omsorgsmeldinger
