(ns rosalind.core)
(use 'clojure.java.io)

;; para seguir usando read-lines con clojure 1.4. Ojo que las líneas
;; no pueden contener espacios en blanco (en medio)
(defn read-lines [file]
  (into [] (re-seq #"\S+" (slurp file))))

;; Convierte un fichero con líneas de texto de igual longitud en una matriz n x n
(defn matrix [file] (into [] (map (fn [x] (into [] x)) (map seq (read-lines file)))))

(def dna-str "AGCTTTTCATTCTGACTGCAACGGGCAATATGTCTCTGTGTGGATTAAAAAAAGAGTGTCTGATAGCAGC")

;; problema DNA cuenta de nucleótidos
(defn cuenta [cadena char]
  (cond (or (= cadena "") (= cadena ())) 0
        (= (first cadena) char) (+ 1 (cuenta (rest cadena) char))
        :default (cuenta (rest cadena) char)))

(defn dna [dna-s]
  (do
    (print (cuenta dna-s \A) " ")
    (print (cuenta dna-s \C) " ")
    (print (cuenta dna-s \G) " ") 
    (print (cuenta dna-s \T) " ")
    ))

;; problema RNA string 
(defn rna-string [dna]
  (apply str (map (fn [x] (if (= x \T) \U x)) (seq dna))))


;; REVC reverse complement
(def mapa {\A \T \T \A \C \G \G \C})
(defn revc [dna]
  (apply str (map mapa (reverse dna))))


;; GC. GC Content
(defn gc-c [dnagc]
  "calcula la proporción de GC en cadena DNA"
  (* (/ (+ (cuenta dnagc \G) (cuenta dnagc \C)) (count dnagc) ) 100.0))

(defn make-map-gc [file] 
  "Devuelve mapa key - val"
  ;; convertir el fichero en secuencia de líneas
  (loop [lineas (re-seq #"\S+" (slurp file)) mapa {} nombre nil cadena []]
    ;; asociar key - val hasta consumir
    (if (empty? lineas)
      (assoc mapa (apply str cadena) nombre)
      (let [linea (first lineas)]
        (cond (and (= \> (.charAt linea 0)) (not (= nombre nil)))
              (recur (rest lineas) (assoc mapa (apply str cadena) nombre) (subs linea 1) [])
              (= \> (.charAt linea 0))
              (recur (rest lineas) mapa (subs linea 1) cadena)
              :else
              (recur (rest lineas) mapa nombre (conj cadena linea))))))
  )

(defn make-map-gc-1 [file] ;; esta versión NO funciona para los datasets de rosalind ya son varias líneas para el valor
  "Devuelve mapa key - val"
  ;; convertir el fichero sin \n  en secuencia de líneas
  (loop [lineas (re-seq #"\S+" (slurp file)) mapa {}]
    ;; asociar key - val hasta consumir
    (if (empty? lineas)
      mapa
      (recur (rest (rest lineas)) (assoc mapa (second lineas) (first lineas)))))
  )


(defn greatest-gc [mapa]
  "Devuelve el ID,valor de la cadena con mayor % de GC"
  (let [dnas (keys mapa)
        greatest (last (sort-by gc-c dnas))]
    {(mapa greatest) (gc-c greatest)}))

;; PERM. Permutaciones para n <= 7
;;
;; Para mostrar los elementos lo hice así:
;; (loop [r (perm 3)] (if (empty? r) r (do (apply prn (first r)) (recur (rest r)))))
;; para contarlos y cotejarlos:
;; (count (perm 3))
;; q debe ser igual a 3!
;; (reduce * 1 (range 1 4))
(defn make-mult
  "devuelve una f que genera n copias del elemento e"
  [n]
  (fn [e]
    (for [i (range n)] e)))
;;  (mapcat m3 '([1 2] [2 1]))
;; 
;; (into [] (concat (subvec v 0 2) [3] (subvec v 2 2)))

(defn inserter
  "inserta n a cada elemento de l"
  [n]
  (fn [l]
    (for [i (range (count l))]
      (into [] (concat (subvec (nth l i) 0 (mod i n)) [n] (subvec (nth l i) (mod i n) (count (nth l i))))))))

(defn perm
  "lista las permutaciones del conjunto de enteros positivos 1,2,..., n"
  [n]
  (loop [i 1 perms '([1])]
    (if (= i n)
      perms
      (recur (inc i) ((inserter (inc i)) (mapcat (make-mult (inc i)) perms)))))
)    

(defn write-out
  "Escribe en un fichero los resultados de perm"
  [n]
  (with-open [wrt (writer "src/rosalind/perm-out.txt")]
    (doseq [x (perm n)]
      (.write wrt (with-out-str
                    (println (apply str (map (fn [e] (str e " ")) x))))))))


;; PROB. Probabilidad a partir GC, ¿dos iguales?
(defn cal-prob
  [l]
  "soporte de la función prob que, aquí es donde se calcula, así la puede usar EVAL"
  (let [gcs l
        gc #(/ % 2)
        at #(/ (- 1 %) 2)
        res #(+ (* (* (gc %) (gc %)) 2 )
                (* (* (at %) (at %)) 2 )) ]
    (map res gcs)))
  
(defn prob
  "Lee de fichero hasta 20 %GC y calcula probabilidad obtener dos ácidos ="
  [file]
  (let [gcs (map #(Float. %) (re-seq #"\d\.\d+" (slurp file)))]
    (cal-prob gcs)))

;; EVAL. Calcula el 'valor esperado' ...
;; utiliza la función prob para calcular la probabilidad de que dos valores
;; sean iguales, multiplicado por m (longitud del motif) así calcula la prob
;; de que un string de long m salga ... sumado k veces, siendo k calculado
;; a partir de n (longitud del string DNA en el que encontrar los motif's)
;; la primera línea del fichero contiene los valores de m y n
;; la segunda los valores de GC
(defn pow
  "para no depender de clojure.math.numeric-tower. Calcula x elevado a y"
  [x y]
  (loop [acum 1 i y]
    (if (zero? i)
      acum
      (recur (* acum x) (dec i)))))

(defn eval-motif
  [file]
  (let [txt (slurp file)
        [m n] (map #(Integer. %) (re-seq #"\d+" txt))
        gcs (map #(Float. %) (re-seq #"\d\.\d+" txt))
        probs (cal-prob gcs)]
  (map #(* (pow % m) (inc (- n m))) probs)))
  
;; HAMM. distancia de hamming entre dos cadenas de dna s, t
(defn hamm [s t]
  (reduce + (map (fn [x y] (if (= x y) 0 1)) (seq s) (seq t))))

;; PROT. protein translation
(def prot-map {
"UUU" "F"      "CUU" "L"      "AUU" "I"      "GUU" "V"
"UUC" "F"      "CUC" "L"      "AUC" "I"      "GUC" "V"
"UUA" "L"      "CUA" "L"      "AUA" "I"      "GUA" "V"
"UUG" "L"      "CUG" "L"      "AUG" "M"      "GUG" "V"
"UCU" "S"      "CCU" "P"      "ACU" "T"      "GCU" "A"
"UCC" "S"      "CCC" "P"      "ACC" "T"      "GCC" "A"
"UCA" "S"      "CCA" "P"      "ACA" "T"      "GCA" "A"
"UCG" "S"      "CCG" "P"      "ACG" "T"      "GCG" "A"
"UAU" "Y"      "CAU" "H"      "AAU" "N"      "GAU" "D"
"UAC" "Y"      "CAC" "H"      "AAC" "N"      "GAC" "D"
"UAA" "Stop"   "CAA" "Q"      "AAA" "K"      "GAA" "E"
"UAG" "Stop"   "CAG" "Q"      "AAG" "K"      "GAG" "E"
"UGU" "C"      "CGU" "R"      "AGU" "S"      "GGU" "G"
"UGC" "C"      "CGC" "R"      "AGC" "S"      "GGC" "G"
"UGA" "Stop"   "CGA" "R"      "AGA" "R"      "GGA" "G"
"UGG" "W"      "CGG" "R"      "AGG" "R"      "GGG" "G"
})


;; SUBS. Finding a MOTIF in DNA
(defn subs-r [s t]
  (loop [pos (.indexOf s t) array [] ]
    (if (= pos -1) array
        (recur (.indexOf s t (inc pos)) (conj array (inc pos)))))
  )

;; CONS. consensus and profile
(def file-cons "src/rosalind/rosalind_cons.txt")
(def nucl-pos {0 \A 1 \C 2 \G 3 \T})
(def pos-nucl {\A 0 \C 1 \G 2 \T 3})
(defn get-column [m c]
  "devuelve la columna c de la matriz m"
    (into [] (map (fn [row] (row c)) m)))

(defn transpose [m]
  "transpone la matriz m"
  (into [] (for [j (range (count (m 0)))] (into [] (for [i (range (count m))] ((m i) j))))))
  
(defn count-nucl [col nucl]
  "cuenta las ocurrencias de nucl en la columna col"
  (count (filter (fn [x] (= x nucl)) col)))

(defn profile [m]
  "construye la matriz profile de m"
  (let [mt (transpose m)]
    (into [] (for [n (range (count nucl-pos))]
               (into [] (for [c mt]
                          (count-nucl c (nucl-pos n))))))))

(defn print-profile [p]
  (dotimes [i (count nucl-pos)]
   (print (str (nucl-pos i) \: \ ))
   (apply prn (p i)))
  )

(defn consensus [p]
  "string consensus a partir de la matriz profile"
  (let [pt (transpose p)]
    (apply str (for [i pt]
           (nucl-pos (.indexOf i (reduce max i)))))))

;;
;; GRPH. Overlap graph
;; utiliza subs-r (find motif in string)
;; utiliza make-map-gc para construir el mapa de keys,vals
(defn suffix
  "sufijo de longitud n de cadena s"
  [n]
  (fn [s]
    (subs s (- (count s) n))))

(defn preffix
  "prefijo de longitud n de cadena s"
  [n]
  (fn [s]
    (subs s 0 n)))

(defn find-prefixes
  "devuelve conjunto de strings en s que son sufijos de t, excluido t"
  [t s]
  (loop [init (difference #{s} #{t})]
(defn grph
  "Calcula la lista de adyacencias de O(3) a partir de mapa"
  "Para cada nodo (key), construye (val) un conjunto con los nodos que son prefijos del sufijo de key"
  [mapa]
  (loop [nodos (keys mapa)
         adyacentes {}]
    (if (empty? nodos)
      adyacentes
      (recur (rest nodos) (find-prefixes (first nodos) (keys mapa)))))
  )
;;;;; main 
(defn -main
  [& args]
  ;;  (dna dna-str))
  ;;  (rna-string dna-str))
  ;; (revc dna-str))
  ;; (revc "AAAACCCGGT"))
  ;; (hamm "GAGCCTACTAACGGGAT" "CATCGTAATGACGGCCT"))
  ;;(apply str (prot "AUGGCCAUGGCGCCCAGAACUGAGAUCAAUAGUACCCGUAUUAACGGGUGA" prot-map)))
  ;; (subs-r "ACGTACGTACGTACGT" "GTA"))
  ;;(doall
   ;;(let [p (profile (matrix file-cons))]
   ;;(println (consensus p))
  ;;(print-profile p))))
  (do
    (println (count (perm 3)))
    (loop [r (perm 3)] (if (empty? r) r (do (apply prn (first r)) (recur (rest r)))))))