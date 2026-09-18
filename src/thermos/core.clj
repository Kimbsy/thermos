(ns thermos.core
  (:gen-class)
  (:require [clojure.set :as set])
  (:import (com.fazecast.jSerialComm SerialPort)))

;; oh my god, this would make a great zactronics game

;; https://fazecast.github.io/jSerialComm/javadoc/com/fazecast/jSerialComm/SerialPort.html

;; @TODO: printing images
;; @TODO: printing QR codes
;; @TODO: printing special characters
;; @TODO: restructure to abstract over printer model
;; @TODO: better port selection?

(defn- flag-set?
  [config-value flag]
  (pos? (bit-and config-value flag)))

(def parity
  {SerialPort/NO_PARITY :none
   SerialPort/EVEN_PARITY :even
   SerialPort/ODD_PARITY :odd
   SerialPort/MARK_PARITY :mark
   SerialPort/SPACE_PARITY :space})
(def parity-inv (set/map-invert parity))

(def flow-control
  {SerialPort/FLOW_CONTROL_DISABLED :none
   SerialPort/FLOW_CONTROL_CTS_ENABLED :cts
   SerialPort/FLOW_CONTROL_DSR_ENABLED :dsr
   SerialPort/FLOW_CONTROL_DTR_ENABLED :dtr
   SerialPort/FLOW_CONTROL_RTS_ENABLED :rts
   SerialPort/FLOW_CONTROL_XONXOFF_IN_ENABLED :xin
   SerialPort/FLOW_CONTROL_XONXOFF_OUT_ENABLED :xout})
(def flow-control-inv (set/map-invert flow-control))

(defn parse-flow-control
  [config-value]
  (if (zero? config-value)
    [:none]
    (->> [SerialPort/FLOW_CONTROL_DISABLED
          SerialPort/FLOW_CONTROL_CTS_ENABLED
          SerialPort/FLOW_CONTROL_DSR_ENABLED
          SerialPort/FLOW_CONTROL_DTR_ENABLED
          SerialPort/FLOW_CONTROL_RTS_ENABLED
          SerialPort/FLOW_CONTROL_XONXOFF_IN_ENABLED
          SerialPort/FLOW_CONTROL_XONXOFF_OUT_ENABLED]
         (filter (partial flag-set? config-value))
         (mapv flow-control))))

(defn generate-flow-control
  [flags]
  (->> flags
       (map flow-control-inv)
       (apply bit-or)))

(def wincor-nixdorf-th210-settings
  {:baud 19200
   :data-bits 8
   :stop-bits 1
   :parity :none
   :flow-control-flags [:xin :xout]})

(defn port-details
  [^SerialPort p]
  {:baud (.getBaudRate p)
   :data-bits (.getNumDataBits p)
   :stop-bits (.getNumStopBits p)
   :parity (parity (.getParity p))
   :flow-control-flags (parse-flow-control (.getFlowControlSettings p))})

(defn configure-port
  [^SerialPort p {:keys [baud data-bits stop-bits parity flow-control-flags] :as config}]
  (doto p
    (.setBaudRate baud)
    (.setNumDataBits data-bits)
    (.setNumStopBits stop-bits)
    (.setParity (parity-inv parity))
    (.setFlowControl (generate-flow-control flow-control-flags))
    (.openPort)))

(defn write-text
  "Write some text to the current line"
  [port text]
  (let [bs (byte-array (map byte text))]
    (.writeBytes port bs (count bs))))

(defn write-line
  "Write a line of text"
  [port text]
  (write-text port (str text "\n")))

(def command
  {:init [0x1b 0x40]
   :clear [0x10]
   :print-buffer [0x17]
   :cut [0x19]
   :part-cut [0x1A]
   :line-feed [0x1b 0x64 #_n] ; `n` - number of lines to feed
   :tone [0x1b 0x07]
   :print-test-form [0x1F 0x74]
   :set-column [0x1B 0x14 #_n] ; `n` - column offset (0 index) ; resets after line
   :just-left [0x1B 0x61 0]
   :just-center [0x1B 0x61 1]
   :just-right [0x1B 0x61 2]
   :double-wide-chars [0x12] ; resets after line
   :single-wide-chars [0x13]
   :rotate-0 [0x1B 0x56 0]
   :rotate-90 [0x1B 0x56 1]
   :rotate-270 [0x1B 0x12]
   :upside-down-mode-on [0x1B 0x7B 1]
   :upside-down-mode-off [0x1B 0x7B 0]
   :bold-on [0x1B 0x45 1]
   :bold-off [0x1B 0x45 0]
   :italic-on [0x1b 0x49 1]
   :italic-off [0x1b 0x49 0]
   :font-size-1 [0x1D 0x21 0x00]
   :font-size-2 [0x1D 0x21 0x11] ; seems to reset after line for some reason?
   :font-size-3 [0x1D 0x21 0x22]
   :font-size-4 [0x1D 0x21 0x33]
   :font-size-5 [0x1D 0x21 0x44]
   :font-size-6 [0x1D 0x21 0x55]
   :font-size-7 [0x1D 0x21 0x66]
   :font-size-8 [0x1D 0x21 0x77]
   :invert-bw-on [0x1D 0x42 1]
   :invert-bw-off [0x1D 0x42 0]
   :normal-script [0x1f 0x05 0]
   :sub-script [0x1f 0x05 1]
   :super-script [0x1f 0x05 2]})

(defn write-command
  "Send a control command"
  [port c & args]
  (let [bs (byte-array (concat (command c) args))]
    (.writeBytes port bs (count bs))))

(defn -main
  [& args]
  (prn (SerialPort/getCommPorts))
  (prn "Thermos"))

(comment
  (SerialPort/getCommPorts)

  (def p (last (SerialPort/getCommPorts)))

  (configure-port p wincor-nixdorf-th210-settings)

  (write-line p "hello world")
  )
