(ns rosalind.core
  (:require [clj-http.client :as client]))
(use 'clojure.java.io)
(use 'clojure.set)

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

(defn write-vec
  "Escribe en un fichero los resultados de vec"
  [vec]
  (with-open [wrt (writer "src/rosalind/vector-out.txt")]
    (doseq [x vec]
      (.write wrt (with-out-str
                    (println (apply str (map (fn [e] (str e " ")) x))))))))

(defn write-seq
  "Escribe en un fichero los resultados de seq"
  [seq]
  (with-open [wrt (writer "src/rosalind/seq-out.txt")]
    (doseq [x seq]
      (.write wrt (with-out-str
                    (println x))))))

;; SUBS. Finding a MOTIF in DNA
(defn subs-r [s t]
  (loop [pos (.indexOf s t) array [] ]
    (if (= pos -1) array
        (recur (.indexOf s t (inc pos)) (conj array (inc pos)))))
  )

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

;; REVP. Locating restriction sites
(defn revp
  "Dado una cadena de DNA localiza las posiciones y longitud de cada palíndrome inverso entre 4 y 12 caracteres de longitud"
  [dna]
  (for [i (range (count dna))
        l (range 4 (inc 12))
        :while (not (> (+ i l) (count dna)))
        :when (= (subs dna i (+ i l))
                 (revc (subs dna i (+ i l))))]
    [(inc i) l])
        
  )

;; LCS. Locate longest common substring of the collection
;;
;; genera los substrings de mayor longitud y va buscando progresivamente hasta encontrar uno
;; para en el momento de encontrarlo, si no, busca otro de menor longitud y así sucesivamente
;;
;; (lcs (read-lines "src/rosalind/rosalind_lcs.txt"))
;;
(defn lcs
  "Devuelve UNO de los longest common substrings en la colección de cadenas de la entrada (vector de cadenas)"
  [col]
  ;; ordena de menor a mayor longitud  y trabaja solamente con los substrings del primero, de mayor a menor
  (let [shorter (first (sort-by count col))]
    (first (for [t (reverse (sort-by count (for [i (range (count shorter))
                                          l (range (count shorter) 1 -1)
                                          :when (not (> (+ i l)(count shorter)))]
                                      (subs shorter i (+ i l)))))
          ;; t son TODOS los substrings de s de longitud descendiente hasta 2, ordenados de mayor a menor long
          :when (every? (fn [x] (not (empty? (subs-r x t)))) col)]
      t)))
  
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

(defn grph
  "A partir del mapa construye los adyacentes O(3) de cada nodo"
  [mapg]
  (loop [nodos (keys mapg) adyacentes {}]
    (if (empty? nodos)
      adyacentes
      (recur (rest nodos)
             (assoc adyacentes (get mapg (first nodos))
                    (map #(get mapg %)
                         (filter (fn [item]
                                   (= ((suffix 3) (first nodos))
                                      ((preffix 3) item)))
                                 (difference (set (keys mapg)) #{(first nodos)})))))))
  )

(defn show-grph
  "Muestra el resultado de grph"
  [mapa]
  (print
   (apply str (for [[k v] mapa :when (not (empty? v))]
                (apply str (for [i v] (format "%s %s\n" k i)))))))

;;
;; IPRB. Mendelian inheritance
(defn iprb
  "devuelve probabilidad de progenie con gen dominante entre 3 tipos de organismo "
  [k m n]
  ;; k homozigótico dominantes
  ;; m heterozigóticos
  ;; n homozigóticos recesivos
  (let [todos (+ k m n)
        todos-1 (dec todos)
        pK1 (/ k todos)
        pM1 (/ m todos)
        pN1 (/ n todos)]
    (+
     (* pK1 (/ (dec k) todos-1)) ;; 1 y 2 k
     (* pK1 (/ m todos-1)) ;; 1 k y 2 m
     (* pK1 (/ n todos-1)) ;; 1 k y 2 n
     (* pM1 (/ k todos-1)) ;; 1º m 2º k
     (* 0.75 pM1 (/ (dec m) todos-1)) ;; 1 y 2 m
     (* 0.5 pM1 (/ n todos-1)) ;; 1º m 2º n
     (* pN1 (/ k todos-1)) ;; 1º n 2º k
     (* 0.5 pN1 (/ m todos-1)) ;; 1º n 2º m
     ))
        
  )

;;
;; IEV. Calculating expected offspring
(defn iev
  "Dados seis enteros positivos entre 1 y 20000 que expresan los pares ... ver en Rosalind"
  [AA-AA AA-Aa AA-aa Aa-Aa Aa-aa aa-aa]
  (+
   (* AA-AA 2)
   (* AA-Aa 2)
   (* AA-aa 2)
   (* Aa-Aa 2 0.75)
   (* Aa-aa 2 0.5)
   (* aa-aa 2 0)
   )
  )

;;
;; LEXF. Enumerating k-mers lexicographically
;; (write-seq (lexf [\R \K \U \C] 4)) así fue como produje el fichero resultado seq-out.txt
(defn lexf
  "Given a collection of at most 10 symbols defining an ordered alphabet, and a positive integer n (n≤10). Return: All strings of length n that can be formed from the alphabet, ordered lexicographically."
  [symbols n]
  (let [m (into [] (repeat n symbols))]
    (for [k (range 0 (Math/pow (count symbols) n))] (apply str (for [l (range 0 n)] ((m l) (mod (int (quot k (Math/pow n (- (dec n) l)))) (count symbols))))))
    )
  )

;;
;; LONG
;;
;; Genome Assembly as Shortest Superstring
;;
;; Given: At most 50 DNA strings of equal length not exceeding 1 kbp (which represent reads deriving from the same strand of a single linear chromosome).
;; The dataset is guaranteed to satisfy the following condition: there exists a unique way to reconstruct the entire chromosome from these reads by gluing together pairs of reads that overlap by more than half their length.
;; Return: A shortest superstring containing all the given strings (thus corresponding to a reconstructed chromosome).

;; función auxiliar
(defn notsuffix
  "De una serie de strings, devuelve los que no son prefijo de ningún otro, al menos en longitud half + 1. long es la longitud de uno cualquiera de los strings, que deben ser iguales"
  [strings long]
  (for [pre strings
        :when (empty?
               (for [suf strings
                     :when (and (not (= pre suf))
                                (not (empty? ;; si hay algún prefijo(pre)=sufijos(suf)
                                      (for [i (range (/ long 2) long)
                                            :when (= ((preffix i) pre)
                                                     ((suffix i) suf))]
                                        i) ;; lista con las longitudes de prefijo=sufijo
                                      )))]
               pre) ;; en esta lista se depositan los que son pre de algún suf
               )]
    pre))

;; función auxiliar para obtener el prefijo/sufijo de dos strings
(defn longestpr
  "Para un string s, devuelve el mayor substring de t para el que pre(t)=suf(s) o nil"
  [s t]
  (last
   (sort-by count
            (for [i (range 1 (inc (count s)))
                  :let [x ((preffix i) t)
                        y ((suffix i) s)]
                  :when (= x y)]
              x))))

;; f auxiliar, obtiene el string de coll con mayor prefijo de s
(defn longest-from
  [s coll]
  (last
   (sort-by (fn [item] (count (longestpr s item)))
            coll)))
            
  
(defn long-assembly
  "Utilizando las dos funciones auxiliares anteriores, va localizando sucesivamente los strings ensamblables según pre=suf"
  [strings]
  (let [init (first (notsuffix strings (count (strings 0))))] ;; localiza el primer string
    (loop [assembly init
           actual init ;; string localizado como siguiente sobre el que buscar prefijo
           toexplore (into [] (disj (set strings) init))] ;; partimos de todos menos el localizado
      (if (empty? toexplore)
        assembly
        (let [next (longest-from actual toexplore)]
          (recur (clojure.string/replace assembly (longestpr actual next) next)
                 next
                 (into [] (disj (set toexplore) next))))))))
         
;; MPRT.
;; Finding a Protein Motif
;; Given: At most 15 UniProt Protein Database access IDs.
;; Return: For each protein possessing the N-glycosylation motif, output its given access ID followed by a list of locations in the protein string where the motif can be found.
;; To allow for the presence of its varying forms, a protein motif is represented by a shorthand as follows: [XY] means "either X or Y" and {X} means "any amino acid except X." For example, the N-glycosylation motif is written as N{P}[ST]{P}.

;; función auxiliar para obtener la cadena en formato FASTA desde UniProt. Devuelve una entrada tipo mapa (key,val)
(defn getfasta
  [id]
  (let [lineas (re-seq #"[\S ]+" ;; separa las líneas (incluye espacios en blanco)
                       (:body (client/get
                               (str (last (:trace-redirects (client/get (str "http://www.uniprot.org/uniprot/" id)))) ".fasta"))))]
    {id ;; la key sigue siendo el id original
     (apply str (rest lineas))}))

;; devuelve un mapa donde key es el access id y los valores la lista de posiciones en las que se encuentra el motif
;; esta función no descubre los patrones 'solapados'
(defn mprt-1
  [ids] ;; UniProt access ids
  (for [e (map getfasta ids)
        :let [k (apply key e)
              v (apply val e)
              posiciones (map #(inc (.indexOf v %)) (re-seq  #"N{1}[^P][S|T][^P]" v))]
        :when posiciones]
    {k posiciones}))

(defn submotifs
  "Devuelve secuencia de patrones re localizados y solapados en string s de longitud n"
  [s re n]
  (for [i (range n)
        t (partition n (subs s i))
        :let [p (re-find re (apply str t))]
        :when p]
    p))
  
(defn mprt
  [ids]
  (for [e (map getfasta ids)
        :let [k (apply key e)
              v (apply val e)
              posiciones (map #(inc (.indexOf v %)) (submotifs v #"N[^P][S|T][^P]" 4)) ]
        :when (not (empty? posiciones))]
    {k (sort posiciones)}))
  
;; KMP.
;; Calcula la matriz de fallos de una cadena de DNA
(defn kmp
  "Devuelve el array de fallos P(k) de dna"
  [dna]
  (into [0]
        (for [k (range 1 (count dna))
              :let [l
                    (for [j (range 1 k)
                          :when (= ((preffix j) dna)
                                   ((suffix j) (subs dna 0 k)))]
                      j)]
              ]
          (if (empty? l)
            0
            (apply max l)))))

          
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
  ;; (do
  ;;  (println (count (perm 3)))
  ;;  (loop [r (perm 3)] (if (empty? r) r (do (apply prn (first r)) (recur (rest r)))))))
  )