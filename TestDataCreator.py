from sqlite3 import *


# create an in memory squlite database
def create_db():
    conn = connect(':memory:')
    return conn


def read_SQL_file(db: Connection, filename):
    with open(filename) as f:
        sql_file = f.read()
        db.executescript(sql_file)


def insert(db: Connection, query):
    db.execute(query)


def select(db: Connection, query):
    cursor = db.execute(query)
    return cursor


def peek(db: Connection, n=10):
    get_query = f"SELECT * from stjerne LIMIT {n}"
    cursor = db.execute(get_query)
    for row in cursor:
        print(row)


if __name__ == '__main__':
    db = create_db()
    read_SQL_file(db, 'temp/planeter.sql')
    peek(db)
