CREATE TABLE EMPLOYEE (
  ID         INTEGER PRIMARY KEY,
  ID_PARENT  INTEGER,
  NAME_FIRST VARCHAR(50),
  NAME_LAST  VARCHAR(50),
  INDEX (ID_PARENT),
  FOREIGN KEY (ID_PARENT) REFERENCES EMPLOYEE(ID)
)
  ENGINE = INNODB;

INSERT INTO EMPLOYEE(ID, ID_PARENT, NAME_FIRST, NAME_LAST)
VALUES (1, NULL, 1, 1),
  (2, NULL, 2, 2),
  (3, 2, 3, 3),
  (4, 2, 4, 4),
  (5, 4, 5, 5),
  (6, 1, 6, 6);


DELIMITER $$
CREATE OR REPLACE FUNCTION GET_MANAGER(LEVEL INTEGER, ID INTEGER)
  RETURNS VARCHAR(150)
BEGIN
  DECLARE RESULT VARCHAR(150);
  CALL PROC_GET_MANAGER(LEVEL, ID, RESULT);
  RETURN RESULT;
END$$
DELIMITER ;

DELIMITER $$
SET @@SESSION.MAX_SP_RECURSION_DEPTH = 3;
$$
CREATE OR REPLACE PROCEDURE PROC_GET_MANAGER(IN LEVEL INTEGER, IN ID INTEGER, OUT MANAGER VARCHAR(150))
BEGIN
  DECLARE MANAGER_CURSOR CURSOR FOR
    SELECT CONCAT(E.NAME_LAST, ', ', E.NAME_FIRST) AS MANAGER_NAME FROM EMPLOYEE E WHERE E.ID = ID;
  IF ID IS NULL
  THEN
    SET MANAGER := NULL;
  ELSE
    IF LEVEL = 1
    THEN
      OPEN MANAGER_CURSOR;
      FETCH MANAGER_CURSOR INTO MANAGER;
      CLOSE MANAGER_CURSOR;
    ELSE
      SET LEVEL = LEVEL - 1;
      SET ID = (SELECT E.ID_PARENT FROM EMPLOYEE E WHERE E.ID = ID);
      CALL PROC_GET_MANAGER(LEVEL, ID, MANAGER);
    END IF;
  END IF;
END;

SELECT ID,
  NAME_FIRST,
  NAME_LAST,
  GET_MANAGER(1, ID_PARENT) AS MANAGER_L1,
  GET_MANAGER(2, ID_PARENT) AS MANAGER_L2,
  GET_MANAGER(3, ID_PARENT) AS MANAGER_L3
  FROM EMPLOYEE E;