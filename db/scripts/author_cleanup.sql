DO
$$
DECLARE
    lv_row_rel RECORD;
	lv_row_name RECORD;
	author_name VARCHAR;

	cur_name REFCURSOR;
	cur_rel CURSOR for
        select * from relationship where type_id = 1;

BEGIN
    OPEN cur_rel;
    LOOP
        FETCH FROM cur_rel INTO lv_row_rel;
        EXIT WHEN NOT FOUND;
        RAISE NOTICE 'Value: %', lv_row_rel.right_id;

        -- Fetch author name
        OPEN cur_name FOR
            SELECT * FROM person_summary_v psv
            WHERE psv.dspace_object_id = lv_row_rel.right_id;

        FETCH cur_name INTO lv_row_name;

        IF lv_row_name.dpsid IS NULL THEN

            author_name := lv_row_name.first_name || ' ' || lv_row_name.last_name;
            RAISE NOTICE 'Author: %', author_name;



            INSERT INTO metadatavalue (
                SELECT NEXTVAL('metadatavalue_seq'), 10, author_name, null, lv_row_rel.left_place, null, -1, lv_row_rel.left_id
            );

            --DELETE FROM relationship WHERE CURRENT OF cur_rel;
            --RAISE NOTICE 'DELETED ROW';
        
        END IF;

        CLOSE cur_name;
    END LOOP;

END
$$



