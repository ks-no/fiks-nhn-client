package no.ks.fiks.nhn.msh

class GpNotFoundException(message: String, val patientId: String) : MshException(message)